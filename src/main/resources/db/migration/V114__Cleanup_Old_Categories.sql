-- V114: 简化分类，仅保留默认分类
-- 1. 确保所有文章都归类到默认分类 (ID=1, Technology)
UPDATE `blog_post` SET `category_id` = 1 WHERE `category_id` IS NOT NULL AND `category_id` <> 1;

-- 2. 删除除默认分类 (ID=1) 之外的所有其他分类
DELETE FROM `blog_category` WHERE `id` <> 1;
