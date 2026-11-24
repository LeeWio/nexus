-- Correct the public source metadata only when it still contains the seeded placeholder.
UPDATE blog_project
SET github_url = 'https://github.com/LeeWio/nexus'
WHERE name = 'Nexus CMS'
  AND repo_name = 'nexus'
  AND github_url = 'https://github.com/yourusername/nexus'
  AND is_deleted = 0;

UPDATE blog_project
SET github_url = NULL
WHERE name = 'Starry Weather'
  AND repo_name = 'starry-weather'
  AND github_url = 'https://github.com/yourusername/starry-weather'
  AND is_deleted = 0;
