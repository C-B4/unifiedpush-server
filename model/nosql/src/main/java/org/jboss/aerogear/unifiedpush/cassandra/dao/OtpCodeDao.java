package org.jboss.aerogear.unifiedpush.cassandra.dao;

import org.jboss.aerogear.unifiedpush.cassandra.dao.model.OtpCode;
import org.jboss.aerogear.unifiedpush.cassandra.dao.model.OtpCodeKey;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface OtpCodeDao extends CrudRepository<OtpCode, OtpCodeKey> {
	Optional<OtpCode> findById(OtpCodeKey id);

	OtpCode save(OtpCode entity, InsertOptions options);

	void deleteAll(OtpCodeKey id);
}
