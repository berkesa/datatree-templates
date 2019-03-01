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

/**
 * A document-fragment (root tag, and sub-fragments).
 */
public class Fragment {

	// --- VARIABLES ---

	/**
	 * Type of fragment.
	 */
	public byte type;

	/**
	 * First argument.
	 */
	public String arg;

	/**
	 * Content or second argument.
	 */
	public String content;

	// --- SUB-FRAGMENTS ---

	/**
	 * Array of sub-fragments.
	 */
	public Fragment[] children;
	
}