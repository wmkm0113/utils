/*
 * Licensed to the Nervousync Studio (NSYC) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nervousync.tree.huffman;

import jakarta.annotation.Nonnull;
import org.nervousync.commons.Globals;
import org.nervousync.utils.commons.StringUtils;

import java.util.*;

/**
 * <h2 class="en-US">Huffman Tree</h2>
 * <h2 class="zh-CN">霍夫曼树</h2>
 * .0
 *
 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
 * @version $Revision: 1.0.0 $ $Date: Nov 3, 2017 16:39:41 $
 */
@SuppressWarnings("unused")
public final class HuffmanTree {
	/**
	 * <span class="en-US">Node counter</span>
	 * <span class="zh-CN">节点计数器</span>
	 */
	private int nodeCount = 0;
	/**
	 * <span class="en-US">Root node instance</span>
	 * <span class="zh-CN">根节点实例对象</span>
	 */
	private Node rootNode = null;
	/**
	 * <span class="en-US">Code mapping table</span>
	 * <span class="zh-CN">编码映射表</span>
	 */
	private Hashtable<String, Object> codeMapping = new Hashtable<>();

	/**
	 * <h3 class="en-US">Constructor method for HuffmanTree</h3>
	 * <h3 class="zh-CN">HuffmanTree构造方法</h3>
	 */
	public HuffmanTree() {
	}

	/**
	 * <h3 class="en-US">Private constructor method for HuffmanTree</h3>
	 * <h3 class="zh-CN">HuffmanTree私有构造方法</h3>
	 *
	 * @param codeMapping <span class="en-US">Code mapping table</span>
	 *                    <span class="zh-CN">编码映射表</span>
	 */
	private HuffmanTree(final Hashtable<String, Object> codeMapping) {
		this.codeMapping = codeMapping;
	}

	/**
	 * <h3 class="en-US">Insert huffman node into the current huffman tree</h3>
	 * <h3 class="zh-CN">插入一个霍夫曼节点到当前的霍夫曼树</h3>
	 *
	 * @param huffmanNode <span class="en-US">Huffman node who will insert into the current Huffman tree</span>
	 *                    <span class="zh-CN">即将插入当前霍夫曼树的霍夫曼节点</span>
	 */
	public void insertNode(final Node huffmanNode) {
		if (this.rootNode == null) {
			this.rootNode = huffmanNode;
		} else {
			Node currentNode = this.rootNode;
			Node previousNode = null;

			while (currentNode.getFrequency() < huffmanNode.getFrequency()) {
				previousNode = currentNode;
				if (currentNode.getNextNode() == null) {
					currentNode = null;
					break;
				} else {
					currentNode = currentNode.getNextNode();
				}
			}

			if (previousNode == null) {
				huffmanNode.setNextNode(this.rootNode);
				this.rootNode = huffmanNode;
			} else if (currentNode == null) {
				previousNode.setNextNode(huffmanNode);
			} else {
				previousNode.setNextNode(huffmanNode);
				huffmanNode.setNextNode(currentNode);
			}
		}
		this.nodeCount++;
	}

	/**
	 * <h3 class="en-US">Build code mapping table</h3>
	 * <h3 class="zh-CN">构建编码映射表</h3>
	 */
	public void build() {
		while (this.nodeCount > 1) {
			this.mergeNode();
		}
		this.buildCodeMapping(this.rootNode, Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Static method for construct an immutable canonical Huffman code table based on code lengths.</h3>
	 * <h3 class="zh-CN">静态方法，根据码长构建不可变的规范霍夫曼码表</h3>
	 *
	 * @param lengths <span class="en-US">An array of code lengths indexed by symbol, with values ranging from 0 to 30</span>
	 *                <span class="zh-CN">按符号下标存储的码长数组，范围为 0 至 30</span>
	 * @return <span class="en-US">Immutable Canonical Huffman Code Table</span>
	 * <span class="zh-CN">不可变规范霍夫曼码表</span>
	 */
	public static Book canonicalCodes(final int[] lengths) {
		return new Book(lengths);
	}

	/**
	 * <h3 class="en-US">Static method for generate Huffman code lengths with a maximum length constraint, based on the frequencies of integer symbols.</h3>
	 * <h3 class="zh-CN">静态方法，根据整数符号的频率生成限制最大长度的 Huffman 码长</h3>
	 *
	 * @param frequencies <span class="en-US">An array of non-negative frequencies, indexed by symbol</span>
	 *                    <span class="zh-CN">按符号下标存储的非负频率数组</span>
	 * @param maxLength   <span class="en-US">Maximum code length, with a value range of 1 to 30</span>
	 *                    <span class="zh-CN">最大码长，取值范围为 1 至 30</span>
	 * @return <span class="en-US">An array of code lengths, indexed by symbol; returns an array of all zeros if there are no valid symbols.</span>
	 * <span class="zh-CN">按符号下标存储的码长数组；没有有效符号时返回全零数组</span>
	 * @throws NullPointerException     <span class="en-US">Frequency array is null</span>
	 *                                  <span class="zh-CN">频率数组为空</span>
	 * @throws IllegalArgumentException <span class="en-US">Negative frequencies, invalid code length constraints, or a number of effective symbols exceeding the encoding capacity are present.</span>
	 *                                  <span class="zh-CN">存在负频率、码长限制非法，或有效符号数量超出编码容量</span>
	 */
	public static int[] codeLengths(@Nonnull final int[] frequencies, final int maxLength) {
		Objects.requireNonNull(frequencies, "frequencies");
		if (maxLength < 1 || maxLength > 30) {
			throw new IllegalArgumentException("maxLength must be between 1 and 30");
		}
		int[] weights = frequencies.clone();
		int present = 0;
		long total = 0L;

		for (int weight : weights) {
			if (weight < 0) {
				throw new IllegalArgumentException("Negative symbol frequency");
			}
			if (weight > 0) {
				present++;
			}
			total += weight;
		}

		if (present > (1L << maxLength)) {
			throw new IllegalArgumentException("Alphabet exceeds the code length limit");
		}

		int[] lengths = new int[weights.length];
		if (present == 0) {
			return lengths;
		}

		while (total > Integer.MAX_VALUE) {
			total = rescaleWeights(weights);
		}

		while (true) {
			HuffmanTree tree = new HuffmanTree();
			for (int symbol = 0; symbol < weights.length; symbol++) {
				if (weights[symbol] > 0) {
					tree.insertNode(new Node(Integer.toString(symbol), weights[symbol]));
				}
			}
			while (tree.nodeCount > 1) {
				tree.mergeNode();
			}
			ArrayDeque<Node> nodes = new ArrayDeque<>();
			ArrayDeque<Integer> depths = new ArrayDeque<>();

			nodes.push(tree.rootNode);
			depths.push(0);

			int longest = 0;

			while (!nodes.isEmpty()) {
				Node node = nodes.pop();
				int depth = depths.pop();

				if (node.keyword != null) {
					int length = Math.max(1, depth);
					lengths[Integer.parseInt(node.keyword)] = length;
					longest = Math.max(longest, length);
				} else {
					nodes.push(node.leftNode);
					depths.push(depth + 1);

					nodes.push(node.rightNode);
					depths.push(depth + 1);
				}
			}
			if (longest <= maxLength) {
				return lengths;
			}
			rescaleWeights(weights);
		}
	}

	/**
	 * <h3 class="en-US">Static method for encoding given content string to huffman tree result string using given code mapping</h3>
	 * <h3 class="zh-CN">静态方法，使用给定的编码映射表将给定的内容字符串编码为霍夫曼结果字符串</h3>
	 *
	 * @param codeMapping <span class="en-US">Code mapping table</span>
	 *                    <span class="zh-CN">编码映射表</span>
	 * @param content     <span class="en-US">Content string</span>
	 *                    <span class="zh-CN">内容字符串</span>
	 * @return <span class="en-US">Generated Huffman result string or zero length string if content string is empty</span>
	 * <span class="zh-CN">生成的霍夫曼树编码字符串，当内容字符串为空字符串时返回长度为0的空字符串</span>
	 */
	public static String encodeString(final Hashtable<String, Object> codeMapping, final String content) {
		HuffmanTree huffmanTree = new HuffmanTree(codeMapping);
		return Optional.ofNullable(huffmanTree.encodeString(content))
				.map(Result::getHuffmanValue)
				.orElse(Globals.DEFAULT_VALUE_STRING);
	}

	/**
	 * <h3 class="en-US">Encode given content string to huffman tree result instance using current code mapping</h3>
	 * <h3 class="zh-CN">使用当前的编码映射表将给定的内容字符串编码为霍夫曼结果实例对象</h3>
	 *
	 * @param content <span class="en-US">Content string</span>
	 *                <span class="zh-CN">内容字符串</span>
	 * @return <span class="en-US">Generated Huffman result instance or null if content string is empty</span>
	 * <span class="zh-CN">生成的霍夫曼结果实例对象，当内容字符串为空字符串时返回null</span>
	 */
	public Result encodeString(final String content) {
		if (StringUtils.isEmpty(content)) {
			return null;
		}

		String string = content;
		StringBuilder stringBuilder = new StringBuilder();

		while (!string.isEmpty()) {
			String keyword = String.valueOf(string.charAt(0));
			stringBuilder.append(this.codeMapping.get(keyword));
			string = string.substring(1);
		}

		return new Result(this.codeMapping, stringBuilder.toString());
	}

	/**
	 * <h3 class="en-US">Build code mapping table</h3>
	 * <h3 class="zh-CN">构建编码映射表</h3>
	 *
	 * @param currentNode <span class="en-US">Current Huffman node</span>
	 *                    <span class="zh-CN">当前霍夫曼节点</span>
	 * @param currentCode <span class="en-US">Code symbol</span>
	 *                    <span class="zh-CN">编码符号</span>
	 */
	private void buildCodeMapping(final Node currentNode, final String currentCode) {
		if (currentNode.getKeyword() != null) {
			this.codeMapping.put(currentNode.getKeyword(), currentCode);
		} else {
			this.buildCodeMapping(currentNode.getLeftNode(), currentCode + "0");
			this.buildCodeMapping(currentNode.getRightNode(), currentCode + "1");
		}
	}

	/**
	 * <h3 class="en-US">Merge huffman node which two frequency was lowest</h3>
	 * <h3 class="zh-CN">合并两个权重最低的节点，并将合并后的节点添加到霍夫曼树中</h3>
	 */
	private void mergeNode() {
		Node leftNode = this.pollNode();
		Node rightNode = this.pollNode();

		if (leftNode != null && rightNode != null) {
			Node mergeNode = new Node(leftNode.getFrequency() + rightNode.getFrequency());
			mergeNode.setLeftNode(leftNode);
			mergeNode.setRightNode(rightNode);
			this.insertNode(mergeNode);
		}
	}

	/**
	 * <h3 class="en-US">Retrieves and removes the head of this queue</h3>
	 * <h3 class="zh-CN">取出并移除当前队列的第一个节点</h3>
	 *
	 * @return <span class="en-US">Retrieved node or returns null if this queue is empty.</span>
	 * <span class="zh-CN">取出的节点，如果队列为空则返回null。</span>
	 */
	private Node pollNode() {
		if (this.rootNode == null) {
			return null;
		}
		Node removeNode = this.rootNode;
		this.rootNode = this.rootNode.getNextNode();
		this.nodeCount--;
		return removeNode;
	}

	/**
	 * <h3 class="en-US">Static method for divide the positive weights by two and round up, updating the weight array in place.</h3>
	 * <h3 class="zh-CN">静态方法，将正权重除以二并向上取整，就地更新权重数组。</h3>
	 *
	 * @param weights <span class="en-US">An array of non-negative weights that need to be updated</span>
	 *                <span class="zh-CN">需要更新的非负权重数组</span>
	 * @return <span class="en-US">Sum of updated weights</span>
	 * <span class="zh-CN">更新后的权重总和</span>
	 */
	private static long rescaleWeights(final int[] weights) {
		long total = 0L;
		for (int symbol = 0; symbol < weights.length; symbol++) {
			if (weights[symbol] > 0) {
				weights[symbol] = (weights[symbol] >>> 1) + (weights[symbol] & 1);
			}
			total += weights[symbol];
		}
		return total;
	}

	/**
	 * <h2 class="en-US">Huffman Node</h2>
	 * <h2 class="zh-CN">霍夫曼节点</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Nov 3, 2017 16:26:45 $
	 */
	public static final class Node {
		/**
		 * <span class="en-US">Keyword</span>
		 * <span class="zh-CN">关键词</span>
		 */
		private String keyword;
		/**
		 * <span class="en-US">Frequency</span>
		 * <span class="zh-CN">权重</span>
		 */
		private int frequency;
		/**
		 * <span class="en-US">Left Node</span>
		 * <span class="zh-CN">左节点</span>
		 */
		private Node leftNode;
		/**
		 * <span class="en-US">Right Node</span>
		 * <span class="zh-CN">右节点</span>
		 */
		private Node rightNode;
		/**
		 * <span class="en-US">Next Node, using for chain table</span>
		 * <span class="zh-CN">下一节点，用于链表</span>
		 */
		private Node nextNode;

		/**
		 * <h3 class="en-US">Constructor method for HuffmanNode</h3>
		 * <h3 class="zh-CN">HuffmanNode构造方法</h3>
		 *
		 * @param frequency <span class="en-US">Frequency</span>
		 *                  <span class="zh-CN">权重</span>
		 */
		public Node(final int frequency) {
			this.frequency = frequency;
		}

		/**
		 * <h3 class="en-US">Constructor method for HuffmanNode</h3>
		 * <h3 class="zh-CN">HuffmanNode构造方法</h3>
		 *
		 * @param keyword   <span class="en-US">Keyword</span>
		 *                  <span class="zh-CN">关键词</span>
		 * @param frequency <span class="en-US">Frequency</span>
		 *                  <span class="zh-CN">权重</span>
		 */
		public Node(final String keyword, final int frequency) {
			this.keyword = keyword;
			this.frequency = frequency;
		}

		/**
		 * <h3 class="en-US">Getter method for keyword</h3>
		 * <h3 class="zh-CN">关键词的Getter方法</h3>
		 *
		 * @return <span class="en-US">Keyword</span>
		 * <span class="zh-CN">关键词</span>
		 */
		public String getKeyword() {
			return this.keyword;
		}

		/**
		 * <h3 class="en-US">Setter method for keyword</h3>
		 * <h3 class="zh-CN">关键词的Setter方法</h3>
		 *
		 * @param keyword <span class="en-US">Keyword</span>
		 *                <span class="zh-CN">关键词</span>
		 */
		public void setKeyword(final String keyword) {
			this.keyword = keyword;
		}

		/**
		 * <h3 class="en-US">Getter method for frequency</h3>
		 * <h3 class="zh-CN">权重的Getter方法</h3>
		 *
		 * @return <span class="en-US">Frequency</span>
		 * <span class="zh-CN">权重</span>
		 */
		public int getFrequency() {
			return this.frequency;
		}

		/**
		 * <h3 class="en-US">Setter method for frequency</h3>
		 * <h3 class="zh-CN">权重的Setter方法</h3>
		 *
		 * @param frequency <span class="en-US">Frequency</span>
		 *                  <span class="zh-CN">权重</span>
		 */
		public void setFrequency(final int frequency) {
			this.frequency = frequency;
		}

		/**
		 * <h3 class="en-US">Getter method for left node</h3>
		 * <h3 class="zh-CN">左节点的Getter方法</h3>
		 *
		 * @return <span class="en-US">Left Node</span>
		 * <span class="zh-CN">左节点</span>
		 */
		public Node getLeftNode() {
			return this.leftNode;
		}

		/**
		 * <h3 class="en-US">Setter method for left node</h3>
		 * <h3 class="zh-CN">左节点的Setter方法</h3>
		 *
		 * @param leftNode <span class="en-US">Left Node</span>
		 *                 <span class="zh-CN">左节点</span>
		 */
		public void setLeftNode(final Node leftNode) {
			this.leftNode = leftNode;
		}

		/**
		 * <h3 class="en-US">Getter method for right node</h3>
		 * <h3 class="zh-CN">右节点的Getter方法</h3>
		 *
		 * @return <span class="en-US">Right Node</span>
		 * <span class="zh-CN">右节点</span>
		 */
		public Node getRightNode() {
			return this.rightNode;
		}

		/**
		 * <h3 class="en-US">Setter method for right node</h3>
		 * <h3 class="zh-CN">右节点的Setter方法</h3>
		 *
		 * @param rightNode <span class="en-US">Right Node</span>
		 *                  <span class="zh-CN">右节点</span>
		 */
		public void setRightNode(final Node rightNode) {
			this.rightNode = rightNode;
		}

		/**
		 * <h3 class="en-US">Getter method for next node</h3>
		 * <h3 class="zh-CN">下一节点的Getter方法</h3>
		 *
		 * @return <span class="en-US">Next Node, using for chain table</span>
		 * <span class="zh-CN">下一节点，用于链表</span>
		 */
		public Node getNextNode() {
			return this.nextNode;
		}

		/**
		 * <h3 class="en-US">Setter method for next node</h3>
		 * <h3 class="zh-CN">下一节点的Setter方法</h3>
		 *
		 * @param nextNode <span class="en-US">Next Node, using for chain table</span>
		 *                 <span class="zh-CN">下一节点，用于链表</span>
		 */
		public void setNextNode(final Node nextNode) {
			this.nextNode = nextNode;
		}
	}

	/**
	 * <h2 class="en-US">Huffman Result</h2>
	 * <h2 class="zh-CN">霍夫曼编码结果</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Nov 6, 2017 14:13:35 $
	 */
	public static final class Result {
		/**
		 * <span class="en-US">Code mapping table</span>
		 * <span class="zh-CN">编码映射表</span>
		 */
		private final Hashtable<String, Object> codeMapping = new Hashtable<>();
		/**
		 * <span class="en-US">Result string</span>
		 * <span class="zh-CN">编码字符串</span>
		 */
		private final String huffmanValue;

		/**
		 * <h3 class="en-US">Constructor method for HuffmanResult</h3>
		 * <h3 class="zh-CN">HuffmanResult构造方法</h3>
		 *
		 * @param codeMapping  <span class="en-US">Code mapping table</span>
		 *                     <span class="zh-CN">编码映射表</span>
		 * @param huffmanValue <span class="en-US">Result string</span>
		 *                     <span class="zh-CN">编码字符串</span>
		 */
		public Result(final Hashtable<String, Object> codeMapping, final String huffmanValue) {
			if (codeMapping != null) {
				this.codeMapping.putAll(codeMapping);
			}
			this.huffmanValue = huffmanValue;
		}

		/**
		 * <h3 class="en-US">Getter method for code mapping</h3>
		 * <h3 class="zh-CN">编码映射表的Getter方法</h3>
		 *
		 * @return <span class="en-US">Code mapping table</span>
		 * <span class="zh-CN">编码映射表</span>
		 */
		public Hashtable<String, Object> getCodeMapping() {
			return this.codeMapping;
		}

		/**
		 * <h3 class="en-US">Getter method for result string</h3>
		 * <h3 class="zh-CN">编码字符串的Getter方法</h3>
		 *
		 * @return <span class="en-US">Result string</span>
		 * <span class="zh-CN">编码字符串</span>
		 */
		public String getHuffmanValue() {
			return this.huffmanValue;
		}
	}

	/**
	 * <h2 class="en-US">Immutable specification of Huffman code tables for integer symbols</h2>
	 * <h2 class="zh-CN">整数符号的不可变规范霍夫曼码表</h2>
	 *
	 * @author Steven Wee	<a href="mailto:wmkm0113@gmail.com">wmkm0113@gmail.com</a>
	 * @version $Revision: 1.0.0 $ $Date: Nov 6, 2017 14:13:35 $
	 */
	public static final class Book {

		/**
		 * <span class="en-US">Code lengths stored by symbol index; a value of zero indicates that the symbol is not included in the encoding.</span>
		 * <span class="zh-CN">按符号下标存储的码长，零表示符号不参与编码。</span>
		 */
		private final int[] lengths;
		/**
		 * <span class="en-US">Encoded values ​​are stored according to symbol index; -1 indicates that the symbol is not included in the encoding.</span>
		 * <span class="zh-CN">按符号下标存储的编码值，-1 表示符号不参与编码。</span>
		 */
		private final int[] codes;
		/**
		 * <span class="en-US">The number of valid symbols corresponding to each code length. Supports code lengths from 1 to 30; the zero index is excluded from the count.</span>
		 * <span class="zh-CN">各码长对应的有效符号数量。支持码长 1 至 30，下标零不参与统计。</span>
		 */
		private final int[] counts = new int[31];
		/**
		 * <span class="en-US">The first canonical code value corresponding to each code length.</span>
		 * <span class="zh-CN">各码长对应的第一个规范编码值。</span>
		 */
		private final int[] firstCodes = new int[31];
		/**
		 * <span class="en-US">The starting index in the `symbols` array for the group of symbols corresponding to each code length.</span>
		 * <span class="zh-CN">各码长对应的符号组在 symbols 数组中的起始坐标。</span>
		 */
		private final int[] firstSymbols = new int[31];
		/**
		 * <span class="en-US">Valid symbols, ordered by code length (ascending) and then by symbol index (ascending).</span>
		 * <span class="zh-CN">按码长升序、符号下标升序排列的有效符号。</span>
		 */
		private final int[] symbols;
		/**
		 * <span class="en-US">The minimum codeword length among valid symbols.</span>
		 * <span class="zh-CN">有效符号中的最小码长。</span>
		 */
		private final int minLength;
		/**
		 * <span class="en-US">The maximum codeword length among valid symbols.</span>
		 * <span class="zh-CN">有效符号中的最大码长。</span>
		 */
		private final int maxLength;

		/**
		 * <h3 class="en-US">Private constructor method for the immutable specification of Huffman code tables for integer symbols</h3>
		 * <h3 class="zh-CN">整数符号的不可变规范霍夫曼码表的私有构造方法</h3>
		 *
		 * @param source <span class="en-US">Array of code lengths indexed by symbol</span>
		 *               <span class="zh-CN">按符号下标存储的码长数组</span>
		 */
		private Book(@Nonnull final int[] source) {
			Objects.requireNonNull(source, "lengths");

			this.lengths = source.clone();
			this.codes = new int[source.length];
			Arrays.fill(this.codes, -1);

			int min = 31, max = 0, present = 0;
			for (int length : this.lengths) {
				if (length < 0 || length > 30) {
					throw new IllegalArgumentException("Invalid code length");
				}
				if (length > 0) {
					this.counts[length]++;
					present++;
					min = Math.min(min, length);
					max = Math.max(max, length);
				}
			}

			if (present == 0) {
				throw new IllegalArgumentException("Empty code book");
			}

			this.minLength = min;
			this.maxLength = max;
			this.symbols = new int[present];

			long code = 0;
			int start = 0;

			for (int length = 1; length <= max; length++) {
				code = (code + this.counts[length - 1]) << 1;
				if (code + this.counts[length] > (1L << length)) {
					throw new IllegalArgumentException("Oversubscribed Huffman tree");
				}
				this.firstCodes[length] = (int) code;
				this.firstSymbols[length] = start;
				start += this.counts[length];
			}

			int[] next = this.firstSymbols.clone();
			for (int symbol = 0; symbol < this.lengths.length; symbol++) {
				int length = this.lengths[symbol];
				if (length > 0) {
					int index = next[length]++;
					this.symbols[index] = symbol;
					this.codes[symbol] = this.firstCodes[length] + index - this.firstSymbols[length];
				}
			}
		}

		/**
		 * <h3 class="en-US">Getter method for the minimum codeword length among valid symbols</h3>
		 * <h3 class="zh-CN">有效符号中的最小码长的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">The minimum codeword length among valid symbols.</span>
		 * <span class="zh-CN">有效符号中的最小码长。</span>
		 */
		public int getMinLength() {
			return this.minLength;
		}

		/**
		 * <h3 class="en-US">Getter method for the maximum codeword length among valid symbols</h3>
		 * <h3 class="zh-CN">有效符号中的最大码长的 Getter 方法</h3>
		 *
		 * @return <span class="en-US">The maximum codeword length among valid symbols.</span>
		 * <span class="zh-CN">有效符号中的最大码长。</span>
		 */
		public int getMaxLength() {
			return this.maxLength;
		}

		/**
		 * <h3 class="en-US">Obtain the low-aligned encoding for a specified symbol.</h3>
		 * <h3 class="zh-CN">获取指定符号的低位对齐编码。</h3>
		 *
		 * @param symbol <span class="en-US">Symbol subscript</span>
		 *               <span class="zh-CN">符号下标</span>
		 * @return <span class="en-US">Encoded value; returns -1 if the symbol is not involved in encoding</span>
		 * <span class="zh-CN">编码值；符号不参与编码时返回 -1</span>
		 */
		public int getCode(final int symbol) {
			return this.codes[symbol];
		}

		/**
		 * <h3 class="en-US">Obtain the code length for a specified symbol.</h3>
		 * <h3 class="zh-CN">获取指定符号的码长。</h3>
		 *
		 * @param symbol <span class="en-US">Symbol subscript</span>
		 *               <span class="zh-CN">符号下标</span>
		 * @return <span class="en-US">Code length; returns zero if the symbol is not involved in encoding.</span>
		 * <span class="zh-CN">码长；符号不参与编码时返回零</span>
		 */
		public int getLength(final int symbol) {
			return this.lengths[symbol];
		}

		/**
		 * <h3 class="en-US">Look up a symbol based on its fully qualified name.</h3>
		 * <h3 class="zh-CN">根据完整的规范编码查找符号。</h3>
		 *
		 * @param code   <span class="en-US">The encoded value is aligned to the low-order bits; the high-order bits must not contain any additional data.</span>
		 *               <span class="zh-CN">低位对齐的编码值，高位不得包含额外数据</span>
		 * @param length <span class="en-US">Effective number of bits in an encoding</span>
		 *               <span class="zh-CN">编码的有效位数</span>
		 * @return <span class="en-US">The index of the matching symbol; returns -1 if no match is found.</span>
		 * <span class="zh-CN">匹配的符号下标；未匹配时返回 -1</span>
		 */
		public int getSymbol(final int code, final int length) {
			if (code < 0 || length < this.minLength || length > this.maxLength) {
				return Globals.DEFAULT_VALUE_INT;
			}

			int offset = code - this.firstCodes[length];
			return offset >= 0 && offset < this.counts[length]
					? this.symbols[this.firstSymbols[length] + offset]
					: Globals.DEFAULT_VALUE_INT;
		}
	}
}
