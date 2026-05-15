package com.entityassist.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * JPA converter mapping {@link LocalDate} to SQL {@link Timestamp}.
 */
@Converter()
public class LocalDateTimestampAttributeConverter implements AttributeConverter<LocalDate, Timestamp>
{
	/**
	 * Creates a converter instance.
	 */
	public LocalDateTimestampAttributeConverter()
	{
		// default constructor
	}

	@Override
	public Timestamp convertToDatabaseColumn(LocalDate attribute)
	{
		return (attribute == null ? null : Timestamp.valueOf(attribute.format(DateTimeFormatter.ISO_DATE_TIME)));
	}

	@Override
	public LocalDate convertToEntityAttribute(Timestamp sqlTimestamp)
	{
		return (sqlTimestamp == null ? null : sqlTimestamp.toLocalDateTime().toLocalDate());
	}
}
