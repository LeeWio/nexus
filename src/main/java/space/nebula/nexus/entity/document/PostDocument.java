package space.nebula.nexus.entity.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "blog_post")
public class PostDocument {

	@Id
	private String id; // Post ID as string

	@Field(type = FieldType.Text, analyzer = "standard")
	private String title;

	@Field(type = FieldType.Keyword)
	private String slug;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String summary;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String content;

	@Field(type = FieldType.Keyword)
	private String authorName;

	@Field(type = FieldType.Keyword)
	private String categoryName;

	@Field(type = FieldType.Keyword)
	private List<String> tags;

	@Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
	private LocalDateTime publishedAt;

	@Field(type = FieldType.Long)
	private Long views;
}
