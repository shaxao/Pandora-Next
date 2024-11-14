
export const models = ["gpt-4o", "gpt-4", "gpt-3.5-turbo", "gpt3.5-turbo-16k", "gpt-4-0314", "gpt-4-0613", "gpt-4-1106-preview", "gpt-4-32k", "gpt-4-turbo", "dall-e-3", "claude-3-5-sonnet-20240620", "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307", "llama3-70b-8192", "deepseek-coder", "deepseek-chat", "SparkDesk-v3.5", "gemini-pro", "gemini-pro-v", "glm-4-all", "qwen-turbo", "qwen-plus", "qwen-max"];
// 后端路由
export const baseUrl = "/api";
// 语音处理默认使用chat2api
export const audioUrl = 'https://api.qipusong.site';
export const audioKey = 'sk-RnucML8POtx8QmxU8d63Cb748aFd46F8B7AfF3Af2bEc70Ac';
// local中保存api信息
export const API_URL = 'apiUrl';
export const API_KEY = 'apiKey';
// cookie中存储用户信息
export const AUTH_TOKEN = 'auth_token';
export const USER_NAME = 'user_name';
export const USER_EAMIL = 'user_email';
// local中存储图片base64
export const USER_AVATAR = 'user_avatar';
export const isOnlyWeb = false;
let isMoreChat = false;
// https://chat.oaifree.com/dad04481-fa3f-494e-b90c-b822128073e5
// gpts数据默认从此直接获取，聊天接口需要在设置页面进行设置，前端尚未写，但是选中默认获取gizmoID作为模型名直接发送，根据API中转自行定义修改，从chatBox.getAttribute('gizmo');获取
let openaiBaseUrl = "https://chat.oaifree.com/dad04481-fa3f-494e-b90c-b822128073e5";
let authToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJzZXNzaW9uX2lkIjoiMGM2aC05TU9BX3BHVVpIanItN3BYZW53UTRpWDQtSUYiLCJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiI3MzY3NzQzMjNAcXEuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWV9LCJodHRwczovL2FwaS5vcGVuYWkuY29tL2F1dGgiOnsicG9pZCI6Im9yZy1hVUV1RkJ6RWd1NkdYOEZhMWN6dG5jRHMiLCJ1c2VyX2lkIjoidXNlci1FTlQzSDRXZVc3OGV6UUhZNFdQT3BQVVkifSwiaXNzIjoiaHR0cHM6Ly9hdXRoMC5vcGVuYWkuY29tLyIsInN1YiI6ImF1dGgwfDYzOTMzM2I5ZGFlMWQzNWE5OGMwMTYxNiIsImF1ZCI6WyJodHRwczovL2FwaS5vcGVuYWkuY29tL3YxIiwiaHR0cHM6Ly9vcGVuYWkub3BlbmFpLmF1dGgwYXBwLmNvbS91c2VyaW5mbyJdLCJpYXQiOjE3MjU1NDY0NDUsImV4cCI6MTcyNjQxMDQ0NSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCBtb2RlbC5yZWFkIG1vZGVsLnJlcXVlc3Qgb3JnYW5pemF0aW9uLnJlYWQgb2ZmbGluZV9hY2Nlc3MiLCJhenAiOiJwZGxMSVgyWTcyTUlsMnJoTGhURTlWVjliTjkwNWtCaCJ9.aYImYeCIoI09WBYEvVPlivypvFhkHTBYqU3rhwABevFy6xIjYSsFv8_QlPWL1QIlZvS6SoEwOpz1w29yrlO39HCPBu4sJUq3yGw8-FVS5tTu_-oCuPlQLOWRlJUukcUOJu9-VnvWvVr5sIYnV0THFmaVPPdccVQGG_Xw9OMEoTJfC5-9r08YPfQkq5jP5wtKvpnUuZ12XJZDRbiwIZPHGrswm0wgnj832EcloYrPYuBMBiuT8vlukTK0oc_uGAjnhY87oWvSqRgHyPO8m6sqTanHTGQeohBOycDXCqILm-OfFwI4RDMkG9fGzTuBlaw8YDDPymLw04VHEcp31FjQyw";
let fileExplare = '';
let activeSessionId = null; // 当前活跃会话的标识符
let currentActiveChatBox = null;

export function setCookie(name, value, days) {
  let expires = "";
  if (days) {
    const date = new Date();
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
    expires = "; expires=" + date.toUTCString();
  }
  document.cookie = name + "=" + (value || "") + expires + "; path=/; Secure; SameSite=Strict";
}

export function getCookie(name) {
  const nameEQ = name + "=";
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
  }
  return null;
}

export function saveToLocalStorage(key, value) {
  localStorage.setItem(key, value);
}

// 从 localStorage 读取的函数
export function loadFromLocalStorage(key) {
  return localStorage.getItem(key);
}

export function clearLocalStorage(key) {
  localStorage.removeItem(key);
}


export function clearCookie(name) {
  setCookie(name, "", 0);
}
let turnstilePromise = null;
async function loadTurnstile() {
  if (turnstilePromise) return turnstilePromise;
  turnstilePromise = new Promise((resolve) => {
    if (window.turnstile) {
      resolve(window.turnstile);
    } else {
      const script = document.createElement('script');
      script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve(window.turnstile);
      document.head.appendChild(script);
    }
  });
  return turnstilePromise;
}


export async function _turnstileCb() {
  console.log('_turnstileCb called');
  const turnstile = await loadTurnstile();
  const uniqueId = `turnstile-widget-${Date.now()}`;
  const widgetContainer = document.createElement('div');
  widgetContainer.id = uniqueId;
  document.body.appendChild(widgetContainer);
  return new Promise((resolve, reject) => {
    turnstile.render(`#${uniqueId}`, {
      sitekey: '0x4AAAAAAAc7WTmVHjc1bpgR',
      retry: 'never',
      'error-callback': function (error) {
        console.log(`error ${error}`);
        reject(error);
      },
      callback: function (token) {
        console.log(`access_token ${token}`);
        resolve(token);
      }
    });
  });
}

/**
  * 滚轮监控
  */

// 创建按钮元素
export function scrollChat() {
  let scrollButton = document.createElement('button');
  scrollButton.innerHTML = `
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24"
        class="icon-md m-1 text-token-text-primary">
        <path fill="currentColor" fill-rule="evenodd"
        d="M12 21a1 1 0 0 1-.707-.293l-7-7a1 1 0 1 1 1.414-1.414L11 17.586V4a1 1 0 1 1 2 0v13.586l5.293-5.293a1 1 0 0 1 1.414 1.414l-7 7A1 1 0 0 1 12 21"
        clip-rule="evenodd"></path>
    </svg>`;
  scrollButton.classList.add('cursor-pointer', 'absolute', 'z-10', 'rounded-full', 'bg-clip-padding', 'border', 'text-token-text-secondary', 'border-token-border-light', 'right-1/2', 'juice:translate-x-1/2', 'bg-token-main-surface-primary', 'bottom-5');
  scrollButton.style.display = 'none';
  const inputArea = document.querySelector('.input-area');
  const rect = inputArea.getBoundingClientRect();
  scrollButton.style.bottom = `${window.innerHeight - rect.top + 20}px`;;
  // 添加按钮到容器
  const chatBox = getCurrentActiveChatBox();
  const chatContent = document.getElementById('chat-content');
  chatBox.appendChild(scrollButton);

  // 按钮点击事件：滚动到chat-content的底部
  scrollButton.addEventListener('click', function () {
    chatContent.scrollTo({
      top: chatContent.scrollHeight,
      behavior: 'smooth'
    });
  });

  // 检测滚动条位置，以判断是否显示按钮
  chatContent.addEventListener('scroll', function () {
    // console.log('scrool', chatContent.scrollHeight);
    // console.log('chatContent', chatContent.scrollTop);
    // console.log('chatBox', chatContent.clientHeight);
    const isAtBottom = chatContent.scrollHeight - (chatContent.scrollTop + chatContent.clientHeight) < 1;
    scrollButton.style.display = isAtBottom ? 'none' : 'block';
  });
}

export function setIsMoreChat(value) {
  isMoreChat = value;
}

export function getIsMoreChat() {
  return isMoreChat;
}


export function setOpenaiBaseUrl(openaiBaseUrl) {
  openaiBaseUrl = openaiBaseUrl;
}

export function getOpenaiBaseUrl() {
  return openaiBaseUrl;
}

export function setAuthToken(authToken) {
  authToken = authToken;
}

export function getAuthToken() {
  return authToken;
}

export function setActiveSessionId(sessionId) {
  activeSessionId = sessionId;
}

export function setCurrentActiveChatBox(chatBox) {
  currentActiveChatBox = chatBox;
}

export function getActiveSessionId() {
  return activeSessionId;
}

export function getCurrentActiveChatBox() {
  return currentActiveChatBox;
}

export function setFileExplare(value) {
  fileExplare = value;
}

export function getFileExplare() {
  return fileExplare;
}

export class Message {
  constructor() {
    this.messageId = '';
    this.parentId = '';
    this.content = '';
    this.conversationId = '';
    this.model = '';
    this.gizmo = '';
    this.contentType = '';
    this.firstFlag = false;
  }
}

export class GlobalModeSet {
  constructor() {
    this.models = models;
    this.promot = [];
    this.webProxyUrl = "",
      this.accessToken = "";
    this.tempertaure = 0.35;
    this.topP = 0.9;
    this.maxTokens = 4000;
    this.presencePenalty = 0;
    this.frequencyPenalty = 0;
    this.baseUrl = "";
    this.apiKey = "";
  }
}
