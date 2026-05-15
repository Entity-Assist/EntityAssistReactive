package com.entityassist.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;

/**
 * JPA converter mapping {@link LocalDate} to SQL {@link Date}.
 */
@Converter()
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date>, Serializable
{
	/**
	 * Creates a converter instance.
	 */
	public LocalDateAttributeConverter()
	{
		// default constructor
	}

	@Override
	public Date convertToDatabaseColumn(LocalDate locDate)
	{
		return (locDate == null ? null : Date.valueOf(locDate));
	}

	@Override
	public LocalDate convertToEntityAttribute(Date sqlDate)
	{
		return (sqlDate == null ? null : sqlDate.toLocalDate());
	}
}
