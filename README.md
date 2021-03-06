该项目通过人人开源项目修改而来，链接：https://www.renren.io/

**项目结构** 
```
backend
├─db  项目SQL语句
│
├─common 公共模块
│  ├─aspect 系统日志
│  ├─exception 异常处理
│  ├─validator 后台校验
│  └─xss XSS过滤
│ 
├─config 配置信息
│ 
├─modules 功能模块
│  ├─app API接口模块(APP调用)
│  ├─job 定时任务模块
│  ├─oss 文件服务模块
│  └─sys 权限模块
│ 
├─BackendApplication 项目启动类
│  
├──resources 
│  ├─mapper SQL对应的XML文件
│  └─static 静态资源

```

**技术选型：** 
- 核心框架：Spring Boot 2.3.1.RELEASE
- 安全框架：Apache Shiro 1.7.0
- 持久层框架：MyBatis-Plus 3.4.0
- 定时器：Quartz 2.3.2
- 数据库连接池：Druid 1.1.13
- 日志管理：SLF4J 1.7、Log4j
- 页面交互：Vue2.x 
<br> 
