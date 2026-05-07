#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
ADMIN_TOKEN=""
# 生成随机后缀以避免唯一索引冲突
RAND=$(date +%s | tail -c 4)

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}--- 1. 注册/确认用户 ---${NC}"
REG_RES=$(curl -s -X POST "$BASE_URL/auth/register" \
     -H "Content-Type: application/json" \
     -d "{
        \"username\": \"admin\",
        \"password\": \"Password123!\",
        \"email\": \"admin@nexus.com\",
        \"nickname\": \"NexusAdmin\"
     }")
echo $REG_RES | jq .

echo -e "${GREEN}--- 2. 登录获取 JWT ---${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
     -H "Content-Type: application/json" \
     -d '{
        "username": "admin",
        "password": "Password123!"
     }')
ADMIN_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}登录失败${NC}"
    exit 1
fi
echo -e "获取 Token 成功: Bearer ${ADMIN_TOKEN:0:15}..."

echo -e "${GREEN}--- 3. 创建/获取分类 ---${NC}"
CAT_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/categories" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
        \"name\": \"技术分享_$RAND\",
        \"slug\": \"tech_$RAND\",
        \"description\": \"Nexus 官方技术分享\"
     }")
CATEGORY_ID=$(echo $CAT_RESPONSE | jq -r '.data.id')

if [ "$CATEGORY_ID" == "null" ]; then
    CATEGORY_ID=$(curl -s -X GET "$BASE_URL/admin/categories" -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data[0].id')
fi
echo -e "Category ID: $CATEGORY_ID"

echo -e "${GREEN}--- 4. 发布博文 ---${NC}"
POST_SLUG="hello-nexus-$RAND"
POST_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/posts" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
        \"title\": \"Hello Nexus World $RAND\",
        \"slug\": \"$POST_SLUG\",
        \"content\": \"这是我的第 $RAND 篇 Nexus 博文！内容丰富，欢迎交流。\",
        \"summary\": \"Nexus 启航 - 自动化测试发布\",
        \"status\": \"PUBLISHED\",
        \"categoryId\": $CATEGORY_ID
     }")
POST_ID=$(echo $POST_RESPONSE | jq -r '.data.id')
echo -e "Post ID: $POST_ID, Slug: $POST_SLUG"

echo -e "${GREEN}--- 5. 获取公开博文详情 ---${NC}"
curl -s -X GET "$BASE_URL/public/blog/posts/$POST_SLUG" | jq .

echo -e "${GREEN}--- 6. 提交评论 (初始状态为 PENDING) ---${NC}"
SUBMIT_RES=$(curl -s -X POST "$BASE_URL/public/comments" \
     -H "Authorization: Bearer $ADMIN_TOKEN" \
     -H "Content-Type: application/json" \
     -d "{
        \"content\": \"写的不错！系统集成测试通过。随机码: $RAND\",
        \"postId\": $POST_ID
     }")
echo $SUBMIT_RES | jq .

echo -e "${GREEN}--- 7. 管理员审核评论 ---${NC}"
# 获取刚才提交的评论 ID
COMMENT_ID=$(curl -s -X GET "$BASE_URL/admin/comments" \
     -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r ".data.list | .[] | select(.content | contains(\"$RAND\")) | .id")

echo -e "找到待审核评论 ID: $COMMENT_ID"

curl -s -X PATCH "$BASE_URL/admin/comments/$COMMENT_ID/status?status=APPROVED" \
     -H "Authorization: Bearer $ADMIN_TOKEN" | jq .

echo -e "${GREEN}--- 8. 再次获取博文评论树 (验证 APPROVED 状态) ---${NC}"
curl -s -X GET "$BASE_URL/public/comments/post/$POST_ID" | jq .

echo -e "${GREEN}--- 测试结束 ---${NC}"
