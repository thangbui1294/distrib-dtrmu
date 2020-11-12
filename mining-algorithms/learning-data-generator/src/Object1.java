/**
 * Mining Relationship-Based Access Control Policies
 * Copyright (C) 2020 Thang Bui
 * Copyright (C) 2020 Scott D. Stoller
 * Copyright (c) 2020 Stony Brook University
 * Copyright (c) 2020 Research Foundation of SUNY
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
 
package learningdatagenerator;


import java.io.Serializable;
import java.util.Map;

/**
 * this class represents an object in our object model. It consists of type,
 * id of the object, and all of its fields and corresponding values.
 * @author Thang
 */
public class Object1 implements Serializable{
    private String class1;
    // every object has an "id" field.  we store it separately for convenience.
    private String id;
    // map field name to field value, for all fields except id.
    private Map<String,FieldValue> data;
    public Object1(String class1, String id, Map<String, FieldValue> data){
        this.class1 = class1;
        this.id = id;
        this.data = data;
    }
    // accessor and mutator methods
    public void setClass1(String class1){
        this.class1 = class1;
    }
    public String getClass1(){
        return this.class1;
    }
    public void setId(String id){
        this.id = id;
    }
    public String getId(){
        return this.id;
    }
    public Map<String, FieldValue> getData(){
        return this.data;
    }
    public void setData(Map<String, FieldValue> data){
        this.data = data;
    }
}