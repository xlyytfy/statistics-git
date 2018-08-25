package com.rue.stat.ext;


import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.cdoframework.cdolib.data.cdo.CDO;

public class CDOUtility
{
	public static void main(String[] args)
	{
		String json="{\"programmers\":[{\"firstName\":1,\"lastName\":1.2,\"email\":{\"firstName\":\"Jason\",\"lastName\":\"Hunter\",\"email\":\"bbbb\"}}],\"musicians\":[{\"firstName\":\"Eric\",\"lastName\":\"Clapton\",\"instrument\":\"guitar\"},{\"firstName\":\"Sergei\",\"lastName\":\"Rachmaninoff\",\"instrument\":\"piano\"}]}";
		System.out.println(CDOUtility.JSON2CDO(json));
	}
	
    public static CDO JSON2CDO(String strJSON)
    {
    	if(strJSON.startsWith("["))
    	{
    		JSONArray jsonArray=JSONArray.fromObject(strJSON);
    		CDO cdo= new CDO();
    		cdo.setCDOArrayValue("data",JSON2CDOArray(jsonArray));
    		return cdo;
    	}
    	else
    	{
    		JSONObject jsonObject=JSONObject.fromObject(strJSON);
    		return JSON2CDO(jsonObject);
    	}
    }

	private static CDO[] JSON2CDOArray(JSONArray jsonArray)
	{
		CDO[] cdoArr=new CDO[jsonArray.size()];
		for(int i=0;i<jsonArray.size();i++)
		{
			JSONObject jsonObj=jsonArray.getJSONObject(i);
			cdoArr[i]=JSON2CDO(jsonObj);
		}
		return cdoArr;
	}

    public static CDO JSON2CDO(JSONObject obj)
    {
    	CDO cdo=new CDO();
		Iterator it=obj.keys();
		while(it.hasNext())
		{
			String key=(String)it.next();
			Object object=obj.get(key);
			if(object.getClass()==JSONArray.class)
			{
				cdo.setCDOArrayValue(key,JSON2CDOArray((JSONArray)object));
			}
			else if(object.getClass()==Integer.class)
			{
				cdo.setIntegerValue(key,(Integer)object);
			}
			else if(object.getClass()==String.class)
			{
				cdo.setStringValue(key,(String)object);
			}
			else if(object.getClass()==Double.class)
			{
				cdo.setDoubleValue(key,(Double)object);
			}
			else if(object.getClass()==Float.class)
			{
				cdo.setFloatValue(key,(Float)object);
			}
			else if(object.getClass()==Long.class)
			{
				cdo.setLongValue(key,(Long)object);
			}
			else if(object.getClass()==Boolean.class)
			{
				cdo.setBooleanValue(key,(Boolean)object);
			}
			else
			{
				cdo.setCDOValue(key,JSON2CDO((JSONObject)object));
			}
		}
    	return cdo; 
    }
}
