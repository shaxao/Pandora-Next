package com.lou.freegpt.common.acpect;

import cn.hutool.json.JSONUtil;
import com.lou.freegpt.common.anntation.LogInterface;
import com.lou.freegpt.common.manger.AsyncManager;
import com.lou.freegpt.common.manger.factory.AsyncFactory;
import com.lou.freegpt.domain.ChatLog;
import com.lou.freegpt.enums.BusinessType;
import com.lou.freegpt.enums.StatusEnums;
import com.lou.freegpt.utils.IpUtil;
import com.lou.freegpt.utils.ServletUtils;
import com.lou.freegpt.utils.UserDetailsNow;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
@Aspect
public class LogAspect {

    /**
     * 配置切入点
     */
    @Pointcut("@annotation(com.lou.freegpt.common.anntation.LogInterface)")
    public void logPointCut() {

    }

    /**
     * 配置后置通知
     * @param joinPoint
     * @param resultJson
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "resultJson")
    public void logAfter(JoinPoint joinPoint, Object resultJson) {
        handleLog(joinPoint, null, resultJson);
    }

    private void handleLog(JoinPoint joinPoint,final Exception e, Object resultJson) {
        log.info("----开始记录日志-----");
        try {
            LogInterface log = getAnntationLog(joinPoint);
            if(log == null) {
                return;
            }
            ChatLog chatLog = new ChatLog();
            chatLog.setBeginTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:MM:ss")));
            chatLog.setStatus(StatusEnums.ENABLE.getCode());
            if (e != null){
                chatLog.setStatus(StatusEnums.DISABLE.getCode());
                chatLog.setErrorMsg(e.getMessage());
            }

            String ip = IpUtil.getIp(ServletUtils.getRequest());
            chatLog.setReqIp(ip);
            chatLog.setOperLocation(IpUtil.getAddress(ip));
            chatLog.setOperName(UserDetailsNow.getUsername());
            chatLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            chatLog.setMethod(className + "." + methodName);
            if(!methodName.equals("login")) chatLog.setResp(resultJson.toString());
            // 处理注解参数
            getControllerMethodDescription(joinPoint, log, chatLog);
            AsyncManager.me().execute(AsyncFactory.recordOper(chatLog));

        }catch (Exception exp){
            log.info("------通知异常-------");
            log.error("异常信息:{}", exp.getMessage());
        }
    }

    private void getControllerMethodDescription(JoinPoint joinPoint, LogInterface log, ChatLog chatLog) {
        chatLog.setDescription(log.title());
        chatLog.setBusinessType(log.businessType().getCode());
        if(log.isSaveRequestData()){
            setRequestValue(joinPoint, chatLog);
        }
    }

    /**
     * 获取请求的参数，放到log中
     * @param sysOperationLog 操作日志
     */
    private void setRequestValue(JoinPoint joinPoint, ChatLog sysOperationLog) {
        String requestMethod = sysOperationLog.getRequestMethod();
        if (HttpMethod.PUT.name().equals(requestMethod)
                || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs());
            sysOperationLog.setReqParam(StringUtils.substring(params, 0, 2000));
        }
        else {
            Map<?, ?> paramsMap = (Map<?, ?>) ServletUtils.getRequest()
                    .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            sysOperationLog.setReqParam(StringUtils.substring(paramsMap.toString(), 0, 2000));
        }
    }

    /**
     * 获取注解
     * @param joinPoint
     * @return
     */
    private LogInterface getAnntationLog(JoinPoint joinPoint) {
        Signature joinPointSignature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) joinPointSignature;
        Method method = methodSignature.getMethod();
        return method.getAnnotation(LogInterface.class);
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null) {
            for (Object o : paramsArray) {
                if (!isFilterObject(o)) {
                    o = o == null ? "" : o;
                    Object jsonObj = JSONUtil.toJsonStr(o);
                    params.append(jsonObj.toString()).append(" ");
                }
            }
        }
        return params.toString().trim();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    public boolean isFilterObject(final Object o) {
        return o instanceof MultipartFile || o instanceof HttpServletRequest
                || o instanceof HttpServletResponse;
    }

}
