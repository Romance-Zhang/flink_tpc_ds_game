/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.fs;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A {@link Writer} that uses {@code toString()} on the input elements and writes them to
 * the output bucket file separated by newline.
 *
 * @param <T> The type of the elements that are being written by the sink.
 */
@Deprecated
public class StringWriter<T> extends StreamWriterBase<T> {
	private static final long serialVersionUID = 1L;

	private String charsetName;

	private transient Charset charset;

	private final String rowDelimiter;

	private static final String DEFAULT_ROW_DELIMITER = "\n";

	private byte[] rowDelimiterBytes;

	/**
	 * Creates a new {@code StringWriter} that uses {@code "UTF-8"} charset to convert
	 * strings to bytes.
	 */
	public StringWriter() {
		this("UTF-8", DEFAULT_ROW_DELIMITER);
	}

	/**
	 * Creates a new {@code StringWriter} that uses the given charset to convert
	 * strings to bytes.
	 *
	 * @param charsetName Name of the charset to be used, must be valid input for {@code Charset.forName(charsetName)}
	 */
	public StringWriter(String charsetName) {
		this(charsetName, DEFAULT_ROW_DELIMITER);
	}

	/**
	 * Creates a new {@code StringWriter} that uses the given charset and row delimiter to convert
	 * strings to bytes.
	 *
	 * @param charsetName Name of the charset to be used, must be valid input for {@code Charset.forName(charsetName)}
	 * @param rowDelimiter Parameter that specifies which character to use for delimiting rows
	 */
	public StringWriter(String charsetName, String rowDelimiter) {
		this.charsetName = charsetName;
		this.rowDelimiter = rowDelimiter;
	}

	protected StringWriter(StringWriter<T> other) {
		super(other);
		this.charsetName = other.charsetName;
		this.rowDelimiter = other.rowDelimiter;
	}

	@Override
	public void open(FileSystem fs, Path path) throws IOException {
		super.open(fs, path);

		try {
			this.charset = Charset.forName(charsetName);
			this.rowDelimiterBytes = rowDelimiter.getBytes(charset);
		}
		catch (IllegalCharsetNameException e) {
			throw new IOException("The charset " + charsetName + " is not valid.", e);
		}
		catch (UnsupportedCharsetException e) {
			throw new IOException("The charset " + charsetName + " is not supported.", e);
		}
	}

	@Override
	public void write(T element) throws IOException {
		FSDataOutputStream outputStream = getStream();
		outputStream.write(element.toString().getBytes(charset));
		outputStream.write(rowDelimiterBytes);
	}

	@Override
	public StringWriter<T> duplicate() {
		return new StringWriter<>(this);
	}

	String getCharsetName() {
		return charsetName;
	}

	public String getRowDelimiter() {
		return rowDelimiter;
	}
}
