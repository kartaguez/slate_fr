package com.deadrooster.slate.android.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

import com.deadrooster.slate.android.model.Entry;
import com.deadrooster.slate.android.model.Model.Entries.Tags;
import com.deadrooster.slate.android.util.EncodingConverter;

public class SlateRSSParser {

	public static final String NAMESPACE = null;
	private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");

	private int category;

	/**
	 * Parse an xml file.
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public List<Entry> parse(String content, int category) throws IOException {

		List<Entry> entries = null;

		this.category = category;
		InputStream in = new ByteArrayInputStream(content.getBytes());

		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			while (parser.next() != XmlPullParser.END_TAG) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (Tags.CHANNEL.equals(name)) {
					entries = readFeed(parser);
				} else {
					skip(parser);
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			in.close();
		}

		return entries;
	}

	/**
	 * Read the rss file.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private List<Entry> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {

		List<Entry> entries = new ArrayList<Entry>();

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.CHANNEL);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (Tags.ITEM.equals(name)) {
				entries.add(readEntry(parser));
			} else {
				skip(parser);
			}
		}
		
		return entries;
	}

	/**
	 * Read entry.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {

		Entry entry = null;
		String title = null;
		String description = null;
		String preview = null;
		String thumbnailUrl = null;
		String pubDate = null;
		String author = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.ITEM);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (Tags.TITLE.equals(name)) {
				title = readTitle(parser);
				if (title == null) {
					title = "";
				}
			} else if (Tags.DESCRIPTION.toString().equals(name)) {
				description = readDescription(parser);
				if (description == null) {
					description = "";
				}
			} else if (Tags.DEK.toString().equals(name)) {
				preview = readDek(parser);
				if (preview == null) {
					preview = "";
				}
			} else if (Tags.THUMBNAIL.toString().equals(name)) {
				thumbnailUrl = readThumbnailUrl(parser);
				if (thumbnailUrl == null) {
					thumbnailUrl = "";
				}
			} else if (Tags.PUBDATE.toString().equals(name)) {
				pubDate = readPubDate(parser);
				if (pubDate == null) {
					pubDate = "";
				}
			} else if (Tags.AUTHOR.toString().equals(name)) {
				author = readAuthor(parser);
				if (author == null) {
					author = "";
				}
			} else {
				skip(parser);
			}
		}

		entry = new Entry(title, this.category, description, preview, thumbnailUrl, pubDate, author);
		return entry;
	}

	/**
	 * Read title.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException {
		String title = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.TITLE);
		title = readText(parser);
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.TITLE);

		return title;
	}

	/**
	 * Read preview.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String readDek(XmlPullParser parser) throws XmlPullParserException, IOException {
		String preview = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.DEK);
		preview = readText(parser);
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.DEK);

		return preview;
	}

	/**
	 * Read description
	 * @param parser
	 * @return
	 * @throws IOException 
	 * @throws XmlPullParserException 
	 */
	private String readDescription(XmlPullParser parser) throws XmlPullParserException, IOException {

		String description = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.DESCRIPTION);
		description = readText(parser);
		
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.DESCRIPTION);

		// clean content
		if (description != null) {
			description = description.replace("http://www.slate.fr//", "http://");
	
			Matcher m = null;
	
	    	Pattern imageHeight = Pattern.compile("height:\\s?[^;]+;\\s?");
	    	m = imageHeight.matcher(description);
	    	description = m.replaceAll("");
	
	    	Pattern imageWidth = Pattern.compile("width:\\s?[^;]+;\\s?");
	    	m = imageWidth.matcher(description);
	    	description = m.replaceAll("");
	
			Pattern videoHeight = Pattern.compile(" height=\"[^\"]+\"");
	    	m = videoHeight.matcher(description);
	    	description = m.replaceAll("");
	
			Pattern videoWidth = Pattern.compile(" width=\"[^\"]+\"");
	    	m = videoWidth.matcher(description);
	    	description = m.replaceAll("");
	
			final List<String> tagValues = new ArrayList<String>();
//			Pattern.compile("<a(?=\\s|>)(?!(?:[^>=]|=(['\"])(?:(?!\\1).)*\\1)*?\\shref=['\"])[^>]*>.*?<\\/a>",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
			Pattern imageTag = Pattern.compile("<img\\s(?!\\swidth=\"(.+?)\")(.+?)/>");
			m = imageTag.matcher(description);
			while (m.find()) {
				tagValues.add(m.group());
			}
	
			String cleanValue = null;
			for (String value : tagValues) {
				cleanValue = value.substring(0, value.length() - 2) + " width=\"100%\"/>";
				description = description.replace(value, cleanValue);
			}
	
			description = EncodingConverter.toUTF8(description);
		}
		return description;
	}

	/**
	 * Read description
	 * @param parser
	 * @return
	 * @throws IOException 
	 * @throws XmlPullParserException 
	 */
	private String readPubDate(XmlPullParser parser) throws XmlPullParserException, IOException {

		String pubDate = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.PUBDATE);
		pubDate = readText(parser);
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.PUBDATE);

		pubDate = EncodingConverter.readAsUTF8(pubDate);
		return pubDate;
	}

	/**
	 * Read title.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException {

		String author = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.AUTHOR);
		author = readText(parser);
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.AUTHOR);

		return author;
	}

	/**
	 * Read text content.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {

		String result = null;

		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}

		return result;
	}

	/**
	 * Read HTML text content.
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private String readHTMLText(XmlPullParser parser) throws XmlPullParserException, IOException {

		String result = null;

		if (parser.next() == XmlPullParser.TEXT) {
			result = removeTags(parser.getText());
			parser.nextTag();
		}

		return result;
	}


	/**
	 * Read thumbnail url
	 * @param parser
	 * @return
	 * @throws IOException 
	 * @throws XmlPullParserException 
	 */
	private String readThumbnailUrl(XmlPullParser parser) throws XmlPullParserException, IOException {

		String url = null;

		parser.require(XmlPullParser.START_TAG, NAMESPACE, Tags.THUMBNAIL);
		url = parser.getAttributeValue(null, "url");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, NAMESPACE, Tags.THUMBNAIL);

		return url;
	}

	/**
	 * Remove tags from rich HTML text.
	 * @param string
	 * @return
	 */
	private String removeTags(String string) {

		String result = null;

	    if (string == null || string.length() == 0) {
	    	result = string;
	    } else {
	    	Matcher m = REMOVE_TAGS.matcher(string);
	    	result = m.replaceAll("");
	    }

	    return result;
	}

	/**
	 * Skip the following nested tags.
	 * @param parser
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {

		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}

		int depth = 1;
		while (depth != 0) {
			switch(parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			default:
				break;
			}
		}
		
	}
}
