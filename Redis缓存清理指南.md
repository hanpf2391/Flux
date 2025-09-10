# Redis缓存清理脚本

## 问题
之前的Redis序列化配置导致了类型不匹配的问题，需要清理旧的缓存数据。

## 解决方案

### 1. 清理Redis缓存
使用以下方法清理Redis中的旧数据：

#### 方法一：通过API清理
```bash
curl -X POST http://localhost:8080/api/admin/cache/clear
```

#### 方法二：通过Redis CLI清理
```bash
redis-cli DEL golden_spawn_point
```

#### 方法三：清理所有Redis数据（谨慎使用）
```bash
redis-cli FLUSHDB
```

### 2. 重启后端服务
清理缓存后重启后端服务：
```bash
# 停止服务（如果正在运行）
# 然后重新启动
mvn spring-boot:run
```

### 3. 测试功能
访问测试页面验证功能是否正常：
```
http://localhost:5173/test
```

## 修改内容

### 1. Redis配置优化
- 创建了专用的字符串RedisTemplate
- 禁用了Jackson的类型包装以避免序列化问题
- 使用直接的JSON字符串序列化

### 2. CanvasService修改
- 改用字符串RedisTemplate
- 使用ObjectMapper进行JSON序列化/反序列化
- 改进了错误处理

### 3. 新增管理接口
- 添加了清理缓存的管理接口
- 提供了Redis缓存管理功能

## 测试验证

1. **清理旧缓存**
2. **重启后端服务**
3. **访问测试页面**
4. **验证初始位置功能**

这样应该能够解决Redis序列化的问题！