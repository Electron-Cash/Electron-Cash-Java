package org.electroncash.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class HashMap
extends AbstractMap
{
	public class Null
	{
		public Null() {}

		public String toString()
		{
			return "Nullobject";
		}
	}


	class ISet extends AbstractSet implements Set
	{

		Vector vec = null;

		public ISet()
		{

			vec = new Vector();

		}

		public boolean add(Object o)
		{
			vec.addElement(o);
			return true;
		}

		public int size()
		{
			return vec.size();
		}

		public Iterator iterator()
		{
			return new IIterator(vec);
		}
	}

	class IIterator implements Iterator
	{
		int index = 0;
		Vector vec = null;

		public IIterator(Vector ve) {
			vec = ve;
		}

		public boolean hasNext()
		{
			if (vec.size() > index) return true;
			return false;
		}

		public Object next()
		{
			Object o = vec.elementAt(index);
			if (o == Nullobject) o = null;
			index += 1;
			return o;
		}


		public void remove()
		{
			index -= 1;
			vec.removeElementAt(index);
		}
	}



	class Entry
	implements Map.Entry
	{
		public Object key = null;
		public Object value = null;

		public Entry(Object ke, Object valu)
		{
			key = ke;
			value = valu;
		}

		public boolean equals(Object o) {
			if ((value == value) && (key == key)) { return true;
			}
			return false;
		}


		public Object getValue()
		{
			return value;
		}

		public Object getKey()
		{
			return key;
		}

		public int hashCode()
		{
			return value.hashCode() + key.hashCode();
		}


		public Object setValue(Object valu)
		{
			value = valu;
			return this;
		}
	}


	private Hashtable m_HashTable = null;
	private Null Nullobject = null;

	public HashMap()
	{
		Nullobject = new Null();
		m_HashTable = new Hashtable();
	}

	public HashMap(int initialCapacity)
	{
		Nullobject = new Null();
		m_HashTable = new Hashtable(initialCapacity);
	}

	public HashMap(Map t)
	{
		Nullobject = new Null();
		m_HashTable = new Hashtable();
		putAll(t);
	}

	public void clear()
	{
		m_HashTable.clear();
	}

	public Object clone()
	{
		HashMap hm = new HashMap(this);

		return hm;
	}

	public boolean containsKey(Object key)
	{
		if (key == null) key = Nullobject;
		boolean b = m_HashTable.containsKey(key);
		return b;
	}


	public boolean containsValue(Object value)
	{
		if (value == null) value = Nullobject;
		boolean b = m_HashTable.contains(value);
		return b;
	}


	public Set entrySet()
	{
		Object Key = null;
		ISet s = new ISet();
		Enumeration en = m_HashTable.keys();
		while (en.hasMoreElements())
		{
			Key = en.nextElement();
			s.add(new Entry(Key, m_HashTable.get(Key)));
		}
		return s;
	}


	public Object get(Object key)
	{
		if (key == null) { key = Nullobject;
		}
		Object o = m_HashTable.get(key);

		if (o == Nullobject) { o = null;
		}
		return o;
	}

	public boolean isEmpty()
	{
		return m_HashTable.isEmpty();
	}

	public Set keySet()
	{
		ISet s = new ISet();
		Enumeration en = m_HashTable.keys();

		while (en.hasMoreElements())
		{
			s.add(en.nextElement());
		}

		return s;
	}

	public Object put(Object key, Object value)
	{
		if (key == null) key = Nullobject;
		if (value == null) value = Nullobject;
		return m_HashTable.put(key, value);
	}

	public void putAll(Map m)
	{
		Iterator it = m.entrySet().iterator();
		Object key = null;
		Object value = null;

		while (it.hasNext())
		{
			Map.Entry me = (Map.Entry)it.next();
			if (me.getKey() == null) key = Nullobject; else
				key = me.getKey();
			if (me.getValue() == null) value = Nullobject; else
				value = me.getValue();
			m_HashTable.put(key, value);
		}
	}

	public Object remove(Object key)
	{
		return m_HashTable.remove(key);
	}

	public int size()
	{
		return m_HashTable.size();
	}


	public Collection values()
	{
		ISet s = new ISet();
		Enumeration en = m_HashTable.keys();

		while (en.hasMoreElements())
		{
			Object Key = en.nextElement();
			s.add(m_HashTable.get(Key));
		}
		return s;
	}
}
