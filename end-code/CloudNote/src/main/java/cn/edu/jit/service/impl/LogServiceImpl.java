package cn.edu.jit.service.impl;

import cn.edu.jit.entry.Log;
import cn.edu.jit.entry.LogExample;
import cn.edu.jit.global.GlobalFunction;
import cn.edu.jit.mapper.LogMapper;
import cn.edu.jit.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author jitwxs
 * @date 2018/1/14 11:30
 */
@Service
public class LogServiceImpl implements LogService {
    @Autowired
    LogMapper logMapper;

    @Override
    public void saveLog(HttpServletRequest request, Integer type, String title, String uid){
        saveLog(request, null, type, title, uid);
    }

    @Override
    public void saveLog(HttpServletRequest request, Exception ex, Integer type, String title, String uid){
        Log log = new Log();
        log.setId(GlobalFunction.getUUID());
        log.setType(type);
        log.setTitle(title);
        log.setUserId(uid);
        log.setIp(GlobalFunction.getRemoteAddr(request));
        log.setUserAgent(request.getHeader("user-agent"));
        log.setRequestUrl(request.getRequestURI());
        log.setCreateDate(new Date());
        log.setParams(request.getParameterMap());
        log.setMethod(request.getMethod());
        log.setException(GlobalFunction.getStackTraceAsString(ex));

        logMapper.insertSelective(log);
    }

    @Override
    public List<Log> listAll() {
        LogExample logExample = new LogExample();
        return logMapper.selectByExample(logExample);
    }
}
