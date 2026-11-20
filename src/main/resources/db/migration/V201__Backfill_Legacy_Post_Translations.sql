INSERT INTO blog_post_translation (post_id, locale, title, summary, content, auto_summary, toc)
SELECT id, 'zh', title, summary, content, auto_summary, toc
FROM blog_post
WHERE is_deleted = false;
