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

import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Template to Fragment converter / compiler.
 */
public final class FragmentBuilder implements FragmentTypes {

	public static final Fragment compile(String template, String templatePath, long lastModified) {
		Fragment root = new Fragment();
		compile(template, root);
		root.arg = templatePath;
		root.content = Long.toString(lastModified);
		return root;
	}

	private static final int compile(String template, Fragment command) {
		int start = 0;
		int end = template.indexOf("#{");
		LinkedList<Fragment> treeCommands = new LinkedList<>();
		for (;;) {
			Fragment subPrint = new Fragment();
			if (end == -1) {
				end = template.length();
			}
			if (start != end) {
				subPrint.type = STATIC_TEXT;
				subPrint.content = template.substring(start, end);
				treeCommands.add(subPrint);
			}
			if (end == template.length()) {
				break;
			}
			start = end;
			if (end != template.length()) {
				Fragment subCommand = new Fragment();
				start = template.indexOf("}", end) + 1;
				String commandString = template.substring(end + 2, start).trim();
				if (commandString.endsWith("}")) {
					commandString = commandString.substring(0, commandString.length() - 1);
				}
				StringTokenizer st = new StringTokenizer(commandString);
				String commandType = st.nextToken().toLowerCase();
				String subTemplate = template.substring(start);
				boolean endTag = false;
				for (;;) {

					// #{in path} or #{include path}
					// File insertion (can be relative path)
					if (commandType.startsWith("in")) {
						subCommand.type = INSERTABLE_TEMPLATE_FILE;
						subCommand.arg = st.nextToken().replace('\\', '/');
						break;
					}

					// #{ex variable} or #{exists variable}...#{end}
					// It is true that such an element exists
					if (commandType.startsWith("ex")) {
						subCommand.type = CONDITION_TAG_EXISTS;
						subCommand.arg = st.nextToken();
						start += compile(subTemplate, subCommand);
						break;
					}

					// #{!ex variable} or #{!exists variable}...#{end}
					// It is true that such an element does not exist
					if (commandType.startsWith("!ex")) {
						subCommand.type = CONDITION_TAG_NOT_EXISTS;
						subCommand.arg = st.nextToken();
						start += compile(subTemplate, subCommand);
						break;
					}

					// #{eq variable 5} or #{equals variable 5}...#{end}
					// It is true that the value of the variable matches the
					// third parameter
					if (commandType.startsWith("eq")) {
						subCommand.type = CONDITION_TAG_VALUE_EQUALS;
						subCommand.arg = st.nextToken();
						subCommand.content = st.nextToken();
						start += compile(subTemplate, subCommand);
						break;
					}

					// #{!eq variable 5} or #{!equals variable 5}...#{end}
					// It is true that the value of the variable does not match
					// the parameter
					if (commandType.startsWith("!eq")) {
						subCommand.type = CONDITION_TAG_VALUE_NOT_EQUALS;
						subCommand.arg = st.nextToken();
						subCommand.content = st.nextToken();
						start += compile(subTemplate, subCommand);
						break;
					}

					// #{for variable : array}...#{end}
					// #{for variable: array}....#{end}
					// #{for variable array}.....#{end}
					// For cycle on array type JSON structure
					if (commandType.equals("for")) {
						subCommand.type = FOR_CYCLE;
						subCommand.content = st.nextToken().replace(':', ' ').trim();
						subCommand.arg = st.nextToken().replace(':', ' ').trim();
						if (st.hasMoreTokens()) {

							// Colon is optional
							subCommand.arg = st.nextToken();
						}
						start += compile(subTemplate, subCommand);
						break;
					}

					// #{end}
					// The blocks for "for", "exists" and "equals" must be
					// closed with "end"!
					if (commandType.equals("end")) {
						endTag = true;
						break;
					}

					// #{variable}
					// Variable insertion
					subCommand.type = INSERTABLE_VARIABLE;
					subCommand.arg = commandType;
					break;
				}
				if (endTag) {
					break;
				}
				treeCommands.add(subCommand);
				end = template.indexOf("#{", start);
			}
		}
		command.children = new Fragment[treeCommands.size()];
		treeCommands.toArray(command.children);
		return start;
	}

}