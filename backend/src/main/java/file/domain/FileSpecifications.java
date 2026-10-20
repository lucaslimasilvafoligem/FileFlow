package file.domain;

import file.infrastructure.entity.FileEntity;
import file.infrastructure.record.FileSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Monta a query de listagem. Sempre exclui arquivos com soft delete.*/
public final class FileSpecifications {
    private FileSpecifications() {}

    public static Specification<FileEntity> from(FileSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (StringUtils.hasText(c.query())) {
                String pattern = "%" + escapeLike(c.query().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("originalName")), pattern, '\\'));
            }
            if (StringUtils.hasText(c.contentTypePrefix())) {
                String pattern = escapeLike(c.contentTypePrefix().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("contentType")), pattern, '\\'));
            }
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.minSize() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Long>get("size"), c.minSize()));
            }
            if (c.maxSize() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Long>get("size"), c.maxSize()));
            }
            if (c.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), c.createdFrom()));
            }
            if (c.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), c.createdTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Escapa curingas do LIKE para que "50%_off" seja busca literal.*/
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
