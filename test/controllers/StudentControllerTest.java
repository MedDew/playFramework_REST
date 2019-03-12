package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import models.Student;
import models.StudentStore;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import play.libs.Json;
import play.test.WithApplication;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class StudentControllerTest extends WithApplication {

    private static final String BASE_URL = "http://localhost:9000";

    public static String makeRequest(String myUrl, String httpMethod, JSONObject parameters){
        URL url = null;
        try{
            url = new URL(myUrl);
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        }catch(IOException e){
            e.printStackTrace();
        }
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        DataOutputStream dos = null;
        String inputString = null;
        try {
            conn.setRequestMethod(httpMethod);
            if (Arrays.asList("POST", "PUT").contains(httpMethod)) {
                String params = parameters.toString();
                conn.setDoOutput(true);
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(params);
                dos.flush();
                dos.close();
            }

            int respCode = conn.getResponseCode();
            if (respCode != 200 && respCode != 201) {
                String error = inputStreamToString(conn.getErrorStream());
                return error;
            }
            inputString = inputStreamToString(conn.getInputStream());
        }catch(IOException e){
            e.printStackTrace();
        }


        return inputString;
    }

    public static String inputStreamToString(InputStream is){
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try{
            br = new BufferedReader(new InputStreamReader(is));

            while((line = br.readLine()) != null){
                sb.append(line);
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            if(br != null){
                try {
                    br.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    @Test
    public void whenCreatingStudent_ThenReturnTheCreatedUser(){
        Student student = new Student(10, "Billy", "Boy", 13);
        JSONObject jsonObj = new JSONObject(makeRequest(BASE_URL,"POST", new JSONObject(student)));

        assertTrue(jsonObj.getBoolean("isSuccessfull"));

        JSONObject body = jsonObj.getJSONObject("body");
        assertEquals(student.getAge(), body.getInt("age"));

        JSONObject jsonObjCreated = new JSONObject(makeRequest(BASE_URL+"/"+body.getInt("id"),"POST", body));
        ObjectMapper objMapper =new ObjectMapper();
        try{
            JSONObject jsonResponse = jsonObjCreated.getJSONObject("body");
            JsonNode jsonNode = objMapper.readTree(jsonResponse.toString());
            Student studentCreated = Json.fromJson(jsonNode, Student.class);
            assertEquals(studentCreated.getId(), body.getInt("id"));
        }catch(IOException e){
            e.printStackTrace();
        }

        assertEquals(student.getFirstName(), body.getString("firstName"));
        assertEquals(student.getLastName(), body.getString("lastName"));
    }

    @Test
    public void whenUpdateStudent_thenReturnTheUpdatedStudent(){
        Student student = new Student(5, "Mehdi", "Gacem", 41);
        JSONObject createStudent = new JSONObject(makeRequest(BASE_URL, "POST", new JSONObject(student) ));
        JSONObject students = new JSONObject(makeRequest(BASE_URL, "GET", null ));

        JSONArray studentsList = students.getJSONArray("body");
        int nbStudents = studentsList.length();
        JSONObject createdStudent = createStudent.getJSONObject("body");
        assertEquals(student.getLastName(), createdStudent.getString("lastName"));

        //Update student's age
        createStudent.put("lastName", "Gaz");
        JSONObject updateStudent = new JSONObject(makeRequest(BASE_URL, "PUT", createStudent));

        assertFalse(student.getLastName() == updateStudent.getJSONObject("body").getString("lastName"));
        assertTrue(createStudent.getString("lastName").equals(updateStudent.getJSONObject("body").getString("lastName")));
        assertEquals(createStudent.getString("lastName"), updateStudent.getJSONObject("body").getString("lastName"));
    }

    @Test
    public void whenGetStudentList_thenReturnNumberOfStudents(){
        JSONObject jsonObject = new JSONObject(makeRequest(BASE_URL, "GET", null));
        JSONArray body = jsonObject.getJSONArray("body");
        int nbOfStudents = 2;
        assertEquals(body.length(), nbOfStudents);
    }

    @Test
    public void whenDeleteStudent_thenReturnTrue(){
        //Create the Student
        Student student = new Student(56, "Lenny", "Boy", 3);
        JSONObject createdStudent = new JSONObject(makeRequest(BASE_URL, "POST", new JSONObject(student)));
        //Retrieve the Student
        int idCreatedStudent = createdStudent.getJSONObject("body").getInt("id");
        JSONObject foundStudent = new JSONObject(makeRequest(BASE_URL+"/"+idCreatedStudent, "POST", new JSONObject() ) );

        assertTrue(foundStudent.getBoolean("isSuccessfull"));
        //Delete the Student
        makeRequest(BASE_URL+"/"+idCreatedStudent, "DELETE", null);

        //Find the deleted Student
        JSONObject deletedUser = new JSONObject(makeRequest(BASE_URL+"/"+idCreatedStudent, "POST", new JSONObject() ) );

        assertFalse(deletedUser.getBoolean("isSuccessfull"));
    }
}
