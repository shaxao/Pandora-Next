import { _turnstileCb, AUTH_TOKEN, getCookie } from "../common.js";
import { showAlert } from "./component.js";



// 定义基础 API URL
const BASE_URL = '/api';

// 请求拦截器
const requestInterceptors = [];
// 响应拦截器
const responseInterceptors = [];

// 添加请求拦截器
export function addRequestInterceptor(interceptor) {
  requestInterceptors.push(interceptor);
}

// 添加响应拦截器
export function addResponseInterceptor(interceptor) {
  responseInterceptors.push(interceptor);
}

// 处理请求配置
async function handleRequestConfig(config) {
  let currentConfig = { ...config };

  // 依次执行所有请求拦截器
  for (const interceptor of requestInterceptors) {
    currentConfig = await interceptor(currentConfig);
  }

  return currentConfig;
}

// 处理响应数据
async function handleResponse(response, config) {
  let currentResponse = response;

  // 依次执行所有响应拦截器
  for (const interceptor of responseInterceptors) {
    currentResponse = await interceptor(currentResponse, config);
  }

  return currentResponse;
}

// 统一的请求函数
async function request(config) {
  try {
    // 处理请求配置
    const finalConfig = await handleRequestConfig({
      headers: {
        'Content-Type': 'application/json',
        ...config.headers,
      },
      ...config,
    });

    // 构建完整的 URL
    const url = `${BASE_URL}${finalConfig.url}`;

    // 发送请求
    const response = await fetch(url, finalConfig);

    // 检查响应状态
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    // 解析响应数据
    const data = await response.json();

    // 处理响应数据
    const handledResponse = await handleResponse(data, finalConfig);

    return handledResponse;
  } catch (error) {
    // 统一错误处理
    console.error('Request failed:', error);
    throw error;
  }
}

// 导出常用的请求方法
export const http = {
  get: (url, params = {}) => {
    const queryString = new URLSearchParams(params).toString();
    return request({
      method: 'GET',
      url: `${url}${queryString ? `?${queryString}` : ''}`,
    });
  },

  post: (url, data) => {
    return request({
      method: 'POST',
      url,
      body: JSON.stringify(data),
    });
  },

  put: (url, data) => {
    return request({
      method: 'PUT',
      url,
      body: JSON.stringify(data),
    });
  },

  delete: (url) => {
    return request({
      method: 'DELETE',
      url,
    });
  },

  // 自定义请求方法
  request,
};

// 添加默认的请求拦截器
addRequestInterceptor(async (config) => {
  // 添加认证信息
  const token = getCookie(AUTH_TOKEN);
  const turnstile = await _turnstileCb()
  if (token) {
    config.headers = {
      ...config.headers,
      'Authorization': `Bearer ${token}`,
      'turnstile': turnstile
    };
  }
  return config;
});

// 添加默认的响应拦截器
addResponseInterceptor(async (response) => {
  // 处理通用的响应格式
  if (response.code === 401) {
    // 处理未授权情况
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  // if (!response.success) {
  //   throw new Error(response.message || 'Request failed');
  // }

  return response;
});

export async function fetchResults(url, auth_token) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${auth_token}`
    }
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  const data = await response.json();
  return data;
}

export async function fetchGetResults(url, auth_token, turnstile) {
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${auth_token}`,
      'turnstile': turnstile
    }
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  const data = await response.json();
  return data;
}


export async function postResults(url, auth_token, requestJson, turnstile) {
  console.log('Request URL:', url);
  console.log('Request Body:', requestJson);
  console.log('Content-Type:', typeof requestJson);

  var myHeaders = new Headers();
  myHeaders.append("turnstile", turnstile);
  myHeaders.append("Content-Type", "application/json");
  myHeaders.append("Accept", "*/*");
  myHeaders.append("Authorization", `Bearer ${auth_token}`);
  myHeaders.append("Connection", "keep-alive");

  const response = await fetch(url, {
    method: 'POST',
    headers: myHeaders,
    body: requestJson,  // 直接使用，因为已经是字符串了
    redirect: 'follow'
  });

  console.log('Response status:', response.status);
  const data = await response.json();
  console.log('Response data:', data);
  return data;
}

export async function postAudiceResults(url, auth_token, formData, turnstile) {
  // const headers = {
  //   'Authorization': `Bearer ${auth_token}`
  // };
  var myHeaders = new Headers();
  myHeaders.append("Content-Type", "application/json");
  myHeaders.append("Authorization", `Bearer ${auth_token}`);
  myHeaders.append("Accept", "*/*");
  myHeaders.append("Connection", "keep-alive");

  // 仅在turnstile有值时添加到headers中
  if (turnstile) {
    //headers['turnstile'] = turnstile;
    myHeaders.append("turnstile", turnstile);
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: myHeaders,
    body: formData,
    redirect: 'follow'
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const reader = response.body.getReader();
  const stream = new ReadableStream({
    start(controller) {
      return pump();
      function pump() {
        return reader.read().then(({ done, value }) => {
          if (done) {
            controller.close();
            return;
          }
          controller.enqueue(value);
          return pump();
        });
      }
    }
  });

  const audioUrl = URL.createObjectURL(new Blob([await new Response(stream).blob()], { type: 'audio/ogg' }));
  return audioUrl;
}

export async function postResult(url, formData, turnstile) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'turnstile': turnstile
    },
    body: JSON.stringify(formData)
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  return data;
}

export function fetchWithTimeout(url, options, timeout = 10000) {
  return new Promise((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      reject(showAlert('请求超时: ' + timeout + 'ms', false));
    }, timeout);

    fetch(url, options)
      .then(response => {
        clearTimeout(timeoutId);
        resolve(response);
      })
      .catch(error => {
        clearTimeout(timeoutId);
        reject(error);
      });
  });
}



export async function loadImage(url) {
  try {
    const response = await fetch(url);
    const blob = await response.blob();
    return URL.createObjectURL(blob);
  } catch (error) {
    console.error('Failed to load image', error);
    return null;
  }
}

/**
 * 发送文本消息
 * @param {*} data
 * @param {*} url
 * @param {*} authToken
 * @param {*} controller
 * @returns
 */
export async function postMessage({ url, data, authToken, controller, turnstile }) {

  const requestOptions = {
    method: 'POST',
    body: data,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`,
      'Accept': '*/*',
      'Connection': 'keep-alive',
      'turnstile': turnstile
    },
    signal: controller ? controller.signal : undefined
  };

  const resp = await fetch(url, requestOptions);
  return resp;

}
