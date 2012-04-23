import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlMatcher {

	private static final Pattern NAME_PATTERN = Pattern.compile("[<]([^/()]+?)[>]");

	public static Map<String, String> match(String pattern, String url) {
		checkNull(pattern, "pattern");
		checkNull(pattern, "url");

		String regexPattern = toRegex(pattern);
		List<String> names = getNames(pattern);

		Pattern p = Pattern.compile(regexPattern);
		Matcher matcher = p.matcher(url);

		if (matcher.matches()) {
			Map<String, String> map = new LinkedHashMap<String, String>();
			int count = matcher.groupCount();
			for (int i = 0; i < count; i++) {
				map.put(names.get(i), matcher.group(i + 1));
			}
			return map;
		} else {
			return null;
		}
	}

	private static List<String> getNames(String pattern) {
		List<String> list = new ArrayList<String>();
		Matcher matcher = NAME_PATTERN.matcher(pattern);
		while (matcher.find()) {
			String name = matcher.group(1);
			if (name.contains(":")) {
				name = name.substring(0, name.indexOf(":"));
			}
			list.add(name);
		}
		return list;
	}

	private static String toRegex(String pattern) {
		String regex = "\\Q" + pattern.replace("<", "\\E<").replace(">", ">\\Q") + "\\E";
		String end = "/*\\E";
		if (regex.endsWith(end)) {
			regex = regex.substring(0, regex.length() - end.length()) + "\\E.*";
		}
		regex = "^" + regex.replace("\\Q\\E", "") + "$";

		Matcher matcher = NAME_PATTERN.matcher(regex);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String name = matcher.group(1);
			if (name.contains(":")) {
				String p = name.substring(name.indexOf(":") + 1);
				matcher.appendReplacement(sb, "(" + Matcher.quoteReplacement(p) + ")");
			} else {
				matcher.appendReplacement(sb, Matcher.quoteReplacement("([^/]+)"));
			}
		}
		matcher.appendTail(sb);

		return sb.toString();
	}

	private static void checkNull(String obj, String fieldName) {
		if (obj == null) {
			throw new IllegalArgumentException(fieldName + " should not be null");
		}
	}

	public static void main(String[] args) {
		// plain match
		Map<String, String> result = UrlMatcher.match("", "");
		check(result != null && result.isEmpty());
		result = UrlMatcher.match("/", "/");
		check(result != null && result.isEmpty());
		result = UrlMatcher.match("/index", "/index");
		check(result != null && result.isEmpty());
		result = UrlMatcher.match("/index/", "/index/");
		check(result != null && result.isEmpty());

		// item match
		result = UrlMatcher.match("/users/<name>", "/users/freewind");
		check(result != null && result.size() == 1 && result.get("name").equals("freewind"));
		result = UrlMatcher.match("/users/~<name>", "/users/~freewind");
		check(result != null && result.size() == 1 && result.get("name").equals("freewind"));
		result = UrlMatcher.match("/users/<name>/edit", "/users/freewind/edit");
		check(result != null && result.size() == 1 && result.get("name").equals("freewind"));
		result = UrlMatcher.match("/users/<name>/<action>", "/users/freewind/edit");
		check(result != null && result.size() == 2 && result.get("name").equals("freewind") && result.get("action").equals("edit"));
		result = UrlMatcher.match("/users/<name>.<suffix>", "/users/detail.json");
		check(result != null && result.size() == 2 && result.get("name").equals("detail") && result.get("suffix").equals("json"));

		// tail * match
		result = UrlMatcher.match("/users/*", "/users/");
		check(result != null && result.isEmpty());
		result = UrlMatcher.match("/users/*", "/users/freewind");
		check(result != null && result.isEmpty());
		result = UrlMatcher.match("/users/*", "/users/freewind/edit");
		check(result != null && result.isEmpty());

		// regex match
		result = UrlMatcher.match("/users/<id:\\d+>", "/users/123456");
		check(result != null && result.size() == 1 && result.get("id").equals("123456"));
		result = UrlMatcher.match("/users/<id:\\d{2}>", "/users/12");
		check(result != null && result.size() == 1 && result.get("id").equals("12"));
		result = UrlMatcher.match("/users/<id:\\d{2}>", "/users/123");
		check(result == null);
		result = UrlMatcher.match("/users/<id:\\d+>", "/users/123456abc");
		check(result == null);

	}

	private static void check(boolean test) {
		if (!test) {
			throw new RuntimeException("assert failed");
		}
	}
}
