package space.nebula.nexus.repository.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.document.PostDocument;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {

	Page<PostDocument> findByTitleOrSummaryOrContent(String title, String summary, String content, Pageable pageable);

}
