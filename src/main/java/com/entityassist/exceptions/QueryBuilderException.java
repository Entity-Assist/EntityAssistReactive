package com.entityassist.exceptions;

/**
 * Occurs when a query builder exception happens
 */
@SuppressWarnings("unused")
public class QueryBuilderException
		extends Exception
{
	/**
	 * Occurs when a query builder exception happens
	 */
	public QueryBuilderException()
	{
	}

	/**
	 * Occurs when a query builder exception happens
	 *
	 * @param message The exception message
	 */
	public QueryBuilderException(String message)
	{
		super(message);
	}

	/**
	 * Occurs when a query builder exception happens
	 *
	 * @param message The exception message
	 * @param cause   The root cause
	 */
	public QueryBuilderException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Occurs when a query builder exception happens
	 *
	 * @param cause The root cause
	 */
	public QueryBuilderException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Occurs when a query builder exception happens
	 *
	 * @param message            The exception message
	 * @param cause              The root cause
	 * @param enableSuppression  Whether suppression is enabled
	 * @param writableStackTrace Whether the stack trace should be writable
	 */
	public QueryBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
