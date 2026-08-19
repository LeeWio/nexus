-- Tiptap JSON is larger than its visible text. Keep the product limit in the
-- application layer and store the serialized document without VARCHAR truncation.
ALTER TABLE blog_moment MODIFY COLUMN content TEXT NOT NULL;
