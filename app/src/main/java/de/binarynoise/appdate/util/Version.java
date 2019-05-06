package de.binarynoise.appdate.util;

import androidx.annotation.NonNull;

public class Version implements Comparable<Version> {
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private final        int[] version;
	
	public Version(String versionCode) {
		version = parseVersion(versionCode);
	}
	
	/**
	 * copied from com.sun.corba.se.impl.orbutil.ORBUtility included in JDK-10
	 * Parse a version string such as "1.1.6" or "jdk1.2fcs" into
	 * a version array of integers {1, 1, 6} or {1, 2}.
	 * A string of "n." or "n..m" is equivalent to "n.0" or "n.0.m" respectively.
	 */
	private static int[] parseVersion(String version) {
		if (version == null || version.isEmpty())
			return EMPTY_INT_ARRAY;
		char[] s = version.toCharArray(); //the maximum span of the string "n.n.n..." where n is an integer
		int start = 0;
		while (start < s.length && (s[start] < '0' || s[start] > '9'))
			start++;
		int end = start + 1;
		int size = 1;
		while (end < s.length) {
			if (s[end] == '.')
				size++;
			else if (s[end] < '0' || s[end] > '9')
				break;
			end++;
		}
		int[] val = new int[size];
		for (int i = 0; i < size; ++i) {
			int dot = version.indexOf('.', start);
			if (dot == -1 || dot > end)
				dot = end;
			//like "n." or "n..m"
			//equivalent to "n.0" or "n.0.m"
			val[i] = start >= dot ? 0 : Integer.parseInt(version.substring(start, dot));
			start = dot + 1;
		}
		return val;
	}
	
	/**
	 * copied from com.sun.corba.se.impl.orbutil.ORBUtility included in JDK-10
	 * Compare two version arrays.
	 * Return 1, 0 or -1 if v1 is greater than, equal to, or less than v2.
	 */
	@SuppressWarnings("AssignmentToMethodParameter")
	private static int compareVersion(int[] v1, int[] v2) {
		if (v1 == null)
			v1 = EMPTY_INT_ARRAY;
		if (v2 == null)
			v2 = EMPTY_INT_ARRAY;
		for (int i = 0; i < v1.length; ++i) {
			if (i >= v2.length || v1[i] > v2[i]) //is longer or greater than v2
				return 1;
			if (v1[i] < v2[i])
				return -1;
		}
		return v1.length == v2.length ? 0 : -1;
	}
	
	public boolean isNewer(@NonNull Version v) {
		return compareTo(v) > 0;
	}
	
	@Override
	public int compareTo(@NonNull Version v) {
		return compareVersion(version, v.version);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Version))
			return false;
		return compareTo((Version) o) == 0;
	}
	
	@NonNull
	@Override
	public String toString() {
		return getVersionCode();
	}
	
	private String getVersionCode() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < version.length - 1; i++) {
			builder.append(version[i]);
			builder.append(".");
		}
		builder.append(version[version.length - 1]);
		return builder.toString();
	}
}
