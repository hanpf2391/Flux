# SSL证书配置指南

## 1. 创建SSL证书目录
```bash
mkdir -p ssl
```

## 2. 获取SSL证书

### 方式一：Let's Encrypt (推荐)
```bash
# 安装certbot
sudo apt update
sudo apt install certbot

# 获取证书
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# 复制证书到项目目录
sudo cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem ssl/cert.pem
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem ssl/key.pem
sudo chown -R $USER:$USER ssl/
```

### 方式二：自签名证书 (仅用于测试)
```bash
# 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout ssl/key.pem \
    -out ssl/cert.pem \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=Flux/CN=yourdomain.com"
```

### 方式三：购买证书
从CA机构购买证书后，将证书文件复制到ssl目录：
- `ssl/cert.pem` - 证书文件
- `ssl/key.pem` - 私钥文件

## 3. 设置证书权限
```bash
chmod 600 ssl/*
```

## 4. 更新nginx配置
确保nginx.conf中的SSL证书路径正确：
```
ssl_certificate /etc/nginx/ssl/cert.pem;
ssl_certificate_key /etc/nginx/ssl/key.pem;
```

## 5. 自动续期 (Let's Encrypt)
```bash
# 设置定时任务自动续期
sudo crontab -e
# 添加以下行 (每月1号凌晨2点)
0 2 1 * * /usr/bin/certbot renew --quiet --post-hook "docker-compose restart frontend"
```

## 6. Docker Compose环境变量
创建.env文件：
```bash
# .env
DOMAIN_NAME=yourdomain.com
MYSQL_ROOT_PASSWORD=your_secure_root_password
DB_NAME=flux
DB_USERNAME=flux_user
DB_PASSWORD=your_secure_password
REDIS_PASSWORD=your_redis_password
ALLOWED_ORIGINS=https://yourdomain.com
```

## 7. 启动服务
```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 检查服务状态
docker-compose ps
```

## 8. 验证HTTPS
访问 https://yourdomain.com 验证SSL证书是否生效。