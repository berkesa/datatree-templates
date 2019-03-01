/**
 * This software is licensed under the Apache 2 license, quoted below.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at<br>
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0<br>
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datatree.templates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Simple resource / template loader.
 */
public class DefaultLoader implements ResourceLoader {

	@Override
	public String loadTemplate(String templatePath, Charset charset) throws IOException {
		File file = new File(templatePath);
		if (file.isFile()) {
			return readFully(new FileInputStream(file), charset);
		}
		URL url = getClass().getResource(templatePath);
		if (url == null) {
			throw new IOException("Template not found: " + templatePath);
		}
		return readFully(url.openStream(), charset);
	}

	protected static final String readFully(InputStream in, Charset charset) throws IOException {
		byte[] data = new byte[0];
		try {
			byte[] packet = new byte[4096];
			int i;
			int l;
			while ((i = in.read(packet)) != -1) {
				l = data.length + i;
				byte[] s = new byte[l];
				System.arraycopy(data, 0, s, 0, data.length);
				System.arraycopy(packet, 0, s, data.length, i);
				data = s;
			}
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception ingored) {
			}
		}
		return new String(data, charset);
	}

	@Override
	public long lastModified(String templatePath) {
		try {
			File file = new File(templatePath);
			if (file.isFile()) {
				return file.lastModified();
			}
			URL url = getClass().getResource(templatePath);
			String path = url.getFile();
			if (path == null || path.isEmpty()) {
				return -1;
			}
			file = new File(path);
			if (file.isFile()) {
				return file.lastModified();
			}
		} catch (Exception ignored) {
		}
		return -1;
	}

}