package com.entityassist.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * JPA converter mapping {@link LocalDateTime} to SQL {@link Timestamp}.
 */
@Converter()
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp>
{
	/**
	 * Creates a converter instance.
	 */
	public LocalDateTimeAttributeConverter()
	{
		// default constructor
	}

	@Override
	public Timestamp convertToDatabaseColumn(LocalDateTime locDateTime)
	{
		return (locDateTime == null ? null : Timestamp.valueOf(locDateTime));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(Timestamp sqlTimestamp)
	{
		return (sqlTimestamp == null ? null : sqlTimestamp.toLocalDateTime());
	}
}
