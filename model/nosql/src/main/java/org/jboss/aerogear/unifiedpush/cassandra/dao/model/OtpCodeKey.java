package org.jboss.aerogear.unifiedpush.cassandra.dao.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
public class OtpCodeKey implements Serializable {
	private static final long serialVersionUID = -6419944724251681328L;
	public static final String FIELD_VARIANT_ID = "variant_id";
	public static final String FIELD_TOKEN_ID = "token_id";
	private static final Integer MAX_ATTEMPTS = 5;

	@PrimaryKeyColumn(name = FIELD_VARIANT_ID, ordinal = 0, type = PrimaryKeyType.PARTITIONED)
	private UUID variantId;
	@PrimaryKeyColumn(name = FIELD_TOKEN_ID, ordinal = 1, type = PrimaryKeyType.PARTITIONED)
	private String tokenId;
	@PrimaryKeyColumn(name = "code", ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
	private String code;
	@Column(value = "attempts")
	private Integer attempts;
	@Column(value = "locale")
	private String locale;

	public OtpCodeKey() {
		this.attempts = 0;
	}

	public OtpCodeKey(UUID variantId, String tokenId, String code, Locale locale) {
		super();
		this.variantId = variantId;
		this.tokenId = tokenId;
		this.code = code;
		this.attempts = 0;
		this.locale = locale.toLanguageTag();
	}

	public OtpCodeKey(UUID variantId, String tokenId) {
		this(variantId, tokenId, null, Locale.ENGLISH);
	}

	public UUID getVariantId() {
		return variantId;
	}

	public void setVariantId(UUID variantId) {
		this.variantId = variantId;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Locale getLocale() {
		return Locale.forLanguageTag(this.locale);
	}

	public void setLocale(Locale locale) {
		this.locale = locale.toLanguageTag();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tokenId == null) ? 0 : tokenId.hashCode());
		result = prime * result + ((variantId == null) ? 0 : variantId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OtpCodeKey other = (OtpCodeKey) obj;
		if (tokenId == null) {
			if (other.tokenId != null)
				return false;
		} else if (!tokenId.equals(other.tokenId))
			return false;
		if (variantId == null) {
			if (other.variantId != null)
				return false;
		} else if (!variantId.equals(other.variantId))
			return false;
		if (!locale.equals(other.locale))
			return false;
		return true;
	}

	public Integer getAttempts() {
		return this.attempts;
	}

	public void increaseAttempts() {
		this.attempts++;
	}

	public boolean shouldResetOtpCode() {
		return (this.attempts >= MAX_ATTEMPTS);
	}
}
