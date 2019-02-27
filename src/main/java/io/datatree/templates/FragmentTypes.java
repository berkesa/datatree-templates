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
 * Fragment types.
 */
public interface FragmentTypes {

	// Root element
	public static final byte ROOT = 0;

	// Static text
	public static final byte STATIC_TEXT = 1;

	// #{variable}
	public static final byte INSERTABLE_VARIABLE = 2;

	// #{ex variable}
	public static final byte CONDITION_TAG_EXISTS = 3;

	// #{!ex variable}
	public static final byte CONDITION_TAG_NOT_EXISTS = 4;

	// #{eq variable}
	public static final byte CONDITION_TAG_VALUE_EQUALS = 5;

	// #{!eq variable}
	public static final byte CONDITION_TAG_VALUE_NOT_EQUALS = 6;

	// #{for variable}
	public static final byte FOR_CYCLE = 7;

	// #{in path}
	public static final byte INSERTABLE_TEMPLATE_FILE = 8;
	
}
