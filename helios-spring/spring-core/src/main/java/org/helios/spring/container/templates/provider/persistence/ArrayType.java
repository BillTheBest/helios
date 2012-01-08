/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.spring.container.templates.provider.persistence;

import java.io.Serializable;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
* Hibernate type to store Lists of primitives using SQL ARRAY.
* @author Sylvain
*
* References :
* http://forum.hibernate.org/viewtopic.php?t=946973
* http://archives.postgresql.org/pgsql-jdbc/2003-02/msg00141.php
*/
public abstract class ArrayType<T> implements UserType {
   private static final int SQL_TYPE = Types.ARRAY;
   private static final int[] SQL_TYPES = { SQL_TYPE };

   abstract protected Array getDataAsArray(Object value);
   
   abstract protected List<T> getDataFromArray(Object primitivesArray);

   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$BOOLEAN"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="bool[]"
    */
   public static class BOOLEAN extends ArrayType<Boolean>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.BOOLEAN( (List<Boolean>)value );
       }
      
      @Override
      protected List<Boolean> getDataFromArray(Object array){
         boolean[] booleans = (boolean[]) array;
         ArrayList<Boolean> result = new ArrayList<Boolean>( booleans.length );
         for(boolean b: booleans)
            result.add( b );
         
         return result;
      }
   }

   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$INTEGER"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="int[]"
    */
   public static class INTEGER extends ArrayType<Integer>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.INTEGER( (List<Integer>)value );
       }
      
      @Override
      protected List<Integer> getDataFromArray(Object array){
         int[] ints = (int[]) array;
         ArrayList<Integer> result = new ArrayList<Integer>( ints.length );
         for(int i: ints)
            result.add( i );
         
         return result;
      }
   }
   
   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$FLOAT"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="real[]"
    */
   public static class FLOAT extends ArrayType<Float>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.FLOAT( (List<Float>)value );
       }
      
      @Override
      protected List<Float> getDataFromArray(Object array){
         float[] floats = (float[]) array;
         ArrayList<Float> result = new ArrayList<Float>( floats.length );
         for(float f: floats)
            result.add( f );
         
         return result;
      }
   }

   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$DOUBLE"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="float8[]"
    */
   public static class DOUBLE extends ArrayType<Double>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.DOUBLE( (List<Double>)value );
       }
      
      @Override
      protected List<Double> getDataFromArray(Object array){
         double[] doubles = (double[]) array;
         ArrayList<Double> result = new ArrayList<Double>( doubles.length );
         for(double d: doubles)
            result.add( d );
         
         return result;
      }
   }
   
   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$STRING"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="text[]"
    */
   public static class STRING extends ArrayType<String>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.STRING( (List<String>)value );
       }
      
      @Override
      protected List<String> getDataFromArray(Object array){
         String[] strings = (String[]) array;
         ArrayList<String> result = new ArrayList<String>( strings.length );
         for(String s: strings)
            result.add( s );
         
         return result;
      }
   }
   
   /**
    * To use, define :
     * hibernate.property
     *       type="com.seanergie.persistence.ArrayType$DATE"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="timestamp[]"
    */
   public static class DATE extends ArrayType<Date>{
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
           return new SqlArray.DATE( (List<Date>)value );
       }
      
      @Override
      protected List<Date> getDataFromArray(Object array){
         Date[] dates = (Date[]) array;
         ArrayList<Date> result = new ArrayList<Date>( dates.length );
         for(Date d: dates)
            result.add( d );
         
         return result;
      }
   }
   
   /**
    * Warning, this one is special.
    * You have to define a class that extends ENUM_LIST&lt;E&gt; and that has a no arguments constructor.
    * For example : class MyEnumsList extends ENUM_LIST&&ltMyEnumType&gt; { public MyEnumList(){ super( MyEnum.values() ); } }
    * Then, define :
     * hibernate.property
     *       type="com.myPackage.MyEnumsList"
     * hibernate.column
     *       name="fieldName"
     *       sql-type="int[]"
    */
   public static class ENUM<E extends Enum<E>> extends ArrayType<E>{
       private E[] theEnumValues;
      
       /**
        * @param clazz the class of the enum.
        * @param theEnumValues The values of enum (by invoking .values()).
        */
       protected ENUM(E[] theEnumValues) {
           this.theEnumValues = theEnumValues;
       }
      
      @Override
      @SuppressWarnings("unchecked")
       protected Array getDataAsArray(Object value){
         List<E> enums = (List<E>) value;
         List<Integer> integers = new ArrayList<Integer>( enums.size() );
         for(E theEnum: enums)
            integers.add( theEnum.ordinal() );

           return new SqlArray.INTEGER( integers );
       }
      
      @Override
      protected List<E> getDataFromArray(Object array){
         int[] ints = (int[]) array;
         ArrayList<E> result = new ArrayList<E>( ints.length );
         for(int val: ints){
            for(int i=0; i < theEnumValues.length; i++) {
                    if (theEnumValues[i].ordinal() == val) {
                        result.add( theEnumValues[i] );
                        break;
                    }
                }
         }
         
         if( result.size() != ints.length )
            throw new RuntimeException( "Error attempting to convert "+array+" into an array of enums ("+theEnumValues+")." );
         
         return result;
      }
   }
   
   public Class returnedClass(){
      return List.class;
   }
   
   public int[] sqlTypes(){
      return SQL_TYPES;
   }
   
    public Object deepCopy(Object value){
          return value;
    }
   
    public boolean isMutable(){
          return true;
    }
     
    @SuppressWarnings("unused")
    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner)
        throws HibernateException, SQLException {
       
      Array sqlArray = resultSet.getArray(names[0]);
        if( resultSet.wasNull() )
              return null;

        return getDataFromArray( sqlArray.getArray() );
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if( null == value )
            preparedStatement.setNull(index, SQL_TYPE);
        else
              preparedStatement.setArray(index, getDataAsArray(value));
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }
   
    public boolean equals(Object x, Object y) throws HibernateException {
        if( x == y)
            return true;
        if( null == x || null == y )
            return false;
        Class javaClass = returnedClass();
        if( ! javaClass.equals( x.getClass() ) || ! javaClass.equals( y.getClass() ) )
              return false;
       
        return x.equals( y );
    }
   
    @SuppressWarnings("unused")
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
         return cached;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    @SuppressWarnings("unused")
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}

class SqlArray<T> implements Array{
	   private List<T> data;
	   private int baseType;
	   private String baseTypeName = null;
	   
	   protected SqlArray(List<T> data, int baseType){
	      this.data = data;
	      this.baseType = baseType;
	   }
	   
	   protected SqlArray(List<T> data, int baseType, String baseTypeName){
	      this(data, baseType);
	      this.baseTypeName = baseTypeName;
	   }
	   
	   public static class BOOLEAN extends SqlArray<Boolean>{
	      public BOOLEAN(List<Boolean> data){
	         super(data, Types.BIT);
	      }
	   }
	   
	   public static class INTEGER extends SqlArray<Integer>{
	      public INTEGER(List<Integer> data){
	         super(data, Types.INTEGER);
	      }
	   }
	   
	   public static class FLOAT extends SqlArray<Float>{
	      public FLOAT(List<Float> data){
	         super(data, Types.FLOAT);
	      }
	   }
	   
	   public static class DOUBLE extends SqlArray<Double>{
	      public DOUBLE(List<Double> data){
	         super(data, Types.DOUBLE);
	      }
	   }
	   
	   public static class STRING extends SqlArray<String>{
	      public STRING(List<String> data){
	         super(data, Types.VARCHAR, "text");
	      }
	   }
	   
	   public static class DATE extends SqlArray<Date>{
	      public DATE(List<Date> data){
	         super(data, Types.TIMESTAMP);
	      }
	   }
	   
	   public String getBaseTypeName(){
	      if( baseTypeName != null )
	         return baseTypeName;
	      //return SessionFactory..getSettings().getDialect().getTypeName( baseType );
	      return null;
	   }

	   public int getBaseType(){
	      return baseType;
	   }

	   public Object getArray(){
	      return data.toArray();
	   }

	   public Object getArray(long index, int count){
	      int lastIndex = count-(int)index;
	      if( lastIndex > data.size() )
	         lastIndex = data.size();
	      
	      return data.subList((int)(index-1), lastIndex).toArray();
	   }

	   @SuppressWarnings("unused")
	   public Object getArray(Map<String, Class<?>> arg0){
	      throw new UnsupportedOperationException();
	   }

	   @SuppressWarnings("unused")
	   public Object getArray(long arg0, int arg1, Map<String, Class<?>> arg2){
	      throw new UnsupportedOperationException();
	   }

	   public ResultSet getResultSet(){
	      throw new UnsupportedOperationException();
	   }

	   @SuppressWarnings("unused")
	   public ResultSet getResultSet(Map<String, Class<?>> arg0){
	      throw new UnsupportedOperationException();
	   }

	   @SuppressWarnings("unused")
	   public ResultSet getResultSet(long index, int count){
	      throw new UnsupportedOperationException();
	   }

	   @SuppressWarnings("unused")
	   public ResultSet getResultSet(long arg0, int arg1, Map<String, Class<?>> arg2){
	      throw new UnsupportedOperationException();
	   }
	   
	   @Override
	   public String toString(){
	      StringBuilder result = new StringBuilder();
	      result.append('{');
	      boolean first = true;
	      
	      for(T t: data){
	         if( first )
	            first = false;
	         else
	            result.append( ',' );
	         
	         if( t == null ){
	            result.append( "null" );
	            continue;
	         }
	         
	         switch( baseType ){
	            case Types.BIT:
	            case Types.BOOLEAN:
	               result.append(((Boolean)t).booleanValue() ? "true" : "false");
	               break;

	            case Types.INTEGER:
	            case Types.FLOAT:
	            case Types.DOUBLE:
	            case Types.REAL:
	            case Types.NUMERIC:
	            case Types.DECIMAL:
	               result.append( t );
	                break;
	                
	            case Types.VARCHAR:
	               String s = (String)t;
	               // Escape the string
	                 result.append('\"');
	                 for(int p=0; p < s.length(); ++p){
	                     char ch = s.charAt( p );
	                     if( ch == '\0' )
	                         throw new IllegalArgumentException( "Zero bytes may not occur in string parameters." );
	                     if( ch == '\\' || ch == '"' )
	                         result.append('\\');
	                     result.append(ch);
	                 }
	                 result.append('\"');
	                 break;
	                
	            case Types.TIMESTAMP:
	               Date d = (Date)t;
	               result.append('\'');
	               appendDate(result, d);
	               result.append( d );
	               result.append('\'');
	               break;

	            default:
	               throw new UnsupportedOperationException("Unsupported type "+baseType+" / "+getBaseTypeName());
	         }
	      }
	      
	      result.append('}');
	      
	      return result.toString();
	   }
	   
	   private static GregorianCalendar calendar = null;
	   protected void appendDate(StringBuilder sb, Date date){
	      if (calendar == null)
	         calendar = new GregorianCalendar();
	      
	      calendar.setTime( date );
	      
	      // Append Date
	      {
	         int l_year = calendar.get(Calendar.YEAR);
	           // always use at least four digits for the year so very
	           // early years, like 2, don't get misinterpreted
	           //
	           int l_yearlen = String.valueOf(l_year).length();
	           for(int i = 4; i > l_yearlen; i--)
	               sb.append("0");
	   
	           sb.append(l_year);
	           sb.append('-');
	           int l_month = calendar.get(Calendar.MONTH) + 1;
	           if( l_month < 10 )
	               sb.append('0');
	           sb.append(l_month);
	           sb.append('-');
	           int l_day = calendar.get(Calendar.DAY_OF_MONTH);
	           if (l_day < 10)
	               sb.append('0');
	           sb.append(l_day);
	      }      

	      sb.append(' ');
	       
	        // Append Time
	        {
	           int hours = calendar.get(Calendar.HOUR_OF_DAY);
	           if (hours < 10)
	               sb.append('0');
	           sb.append(hours);
	   
	           sb.append(':');
	           int minutes = calendar.get(Calendar.MINUTE);
	           if (minutes < 10)
	               sb.append('0');
	           sb.append(minutes);
	   
	           sb.append(':');
	           int seconds = calendar.get(Calendar.SECOND);
	           if (seconds < 10)
	               sb.append('0');
	           sb.append(seconds);
	   
	           if( date instanceof Timestamp ){
	              // Add nanoseconds.
	              // This won't work for postgresql versions < 7.2 which only want
	              // a two digit fractional second.
	   
	              Timestamp t = (Timestamp) date;
	              char[] decimalStr = {'0', '0', '0', '0', '0', '0', '0', '0', '0'};
	              char[] nanoStr = Integer.toString( t.getNanos() ).toCharArray();
	              System.arraycopy(nanoStr, 0, decimalStr, decimalStr.length - nanoStr.length, nanoStr.length);
	              sb.append('.');
	              sb.append(decimalStr, 0, 6);
	           }
	      }
	       
	        // Append Time Zone offset
	        {
	           //int offset = -(date.getTimezoneOffset());
	              int offset = (calendar.get(Calendar.ZONE_OFFSET)+calendar.get(Calendar.DST_OFFSET)) / (60 * 1000);
	           int absoff = Math.abs(offset);
	           int hours = absoff / 60;
	           int mins = absoff - hours * 60;
	   
	           sb.append((offset >= 0) ? "+" : "-");
	   
	           if (hours < 10)
	               sb.append('0');
	           sb.append(hours);
	   
	           if (mins < 10)
	               sb.append('0');
	           sb.append(mins);
	        }
	       
	        // Append Era
	        if( calendar.get(Calendar.ERA) == GregorianCalendar.BC )
	            sb.append(" BC");
	   }

	public void free() throws SQLException {
	}
}