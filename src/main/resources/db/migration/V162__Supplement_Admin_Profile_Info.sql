-- V162: Supplement Admin Profile Info for just.vireo@gmail.com

UPDATE `sys_user`
SET nickname = 'wei.li',
    github_username = 'LeeWio',
    website = 'https://github.com/LeeWio',
    avatar = 'https://github.com/LeeWio.png',
    bio = 'Co-creator and maintainer of Nexus. Software engineer passionate about building modern web applications.',
    location = 'China',
    updated_at = NOW(),
    last_modified_by = 'SYSTEM'
WHERE email = 'just.vireo@gmail.com' 
  AND is_deleted = false;
