package com.lou.freegpt.common.manger.factory;

import com.lou.freegpt.domain.ChatLog;
import com.lou.freegpt.service.ChatLogService;
import com.lou.freegpt.utils.IpUtil;
import com.lou.freegpt.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.TimerTask;

@Slf4j
public class AsyncFactory {

	/**
	 * 操作日志记录
	 * @param operLog 操作日志信息
	 * @return 任务task
	 */
	public static TimerTask recordOper(final ChatLog operLog) {
		return new TimerTask() {
			@Override
			public void run() {
				String address = IpUtil.getAddress(operLog.getReqIp());
				// 远程查询操作地点
				operLog.setOperLocation(address);
				SpringUtils.getBean(ChatLogService.class).save(operLog);
			}
		};
	}

}
