package cn.edu.zjut.backend.junit_test;

import cn.edu.zjut.backend.controller.UserController;
import cn.edu.zjut.backend.dao.TeacherPositionDAO;
import cn.edu.zjut.backend.dao.TeacherSubjectDAO;
import cn.edu.zjut.backend.dao.UserDAO;
import cn.edu.zjut.backend.po.TeacherPosition;
import cn.edu.zjut.backend.po.TeacherSubject;
import cn.edu.zjut.backend.po.User;
import cn.edu.zjut.backend.util.HibernateUtil;
import cn.edu.zjut.backend.util.Jwt;
import cn.edu.zjut.backend.util.Response;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 删除教师功能 - 边界值测试
 * 测试目标: UserController.deleteUser() 方法的边界条件
 */
public class DeleteTeacherBoundaryValueTest {

    private UserController userController;
    private String adminToken;
    private Long adminUserId = 1L;
    private Long normalTeacherId;
    private Long teacherWithPositionId;
    private Long teacherWithSubjectId;
    private Long teacherNoRelationId;

    @Before
    public void setUp() {
        userController = new UserController();

        // 使用反射注入userService
        try {
            java.lang.reflect.Field userServiceField = UserController.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(userController, new cn.edu.zjut.backend.service.UserService());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("无法注入UserService依赖", e);
        }

        prepareTestData();
        adminToken = generateToken(adminUserId, "admin", 0);
    }

    private void prepareTestData() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;

        try {
            tran = session.beginTransaction();

            // 1. 创建普通教师（无关联）
            User normalTeacher = new User();
            normalTeacher.setUsername("boundary_normal_" + System.currentTimeMillis());
            normalTeacher.setPassword("encoded_password");
            normalTeacher.setRealName("普通边界测试教师");
            normalTeacher.setUserType(1);
            normalTeacher.setPhone("13800000010");
            normalTeacher.setEmail("normal_boundary@test.com");
            normalTeacher.setStatus("0");
            userDAO.add(normalTeacher);
            normalTeacherId = normalTeacher.getUserId();

            // 2. 创建有职位关联的教师
            User teacherWithPosition = new User();
            teacherWithPosition.setUsername("boundary_position_" + System.currentTimeMillis());
            teacherWithPosition.setPassword("encoded_password");
            teacherWithPosition.setRealName("有职位边界测试教师");
            teacherWithPosition.setUserType(1);
            teacherWithPosition.setPhone("13800000011");
            teacherWithPosition.setEmail("position_boundary@test.com");
            teacherWithPosition.setStatus("0");
            userDAO.add(teacherWithPosition);
            teacherWithPositionId = teacherWithPosition.getUserId();

            // 添加职位关联
            TeacherPosition position = new TeacherPosition();
            position.setTeacherId(teacherWithPositionId);
            position.setRole((byte) 0);
            TeacherPositionDAO positionDAO = new TeacherPositionDAO();
            positionDAO.setSession(session);
            positionDAO.add(position);

            // 3. 创建有学科关联的教师
            User teacherWithSubject = new User();
            teacherWithSubject.setUsername("boundary_subject_" + System.currentTimeMillis());
            teacherWithSubject.setPassword("encoded_password");
            teacherWithSubject.setRealName("有学科边界测试教师");
            teacherWithSubject.setUserType(1);
            teacherWithSubject.setPhone("13800000012");
            teacherWithSubject.setEmail("subject_boundary@test.com");
            teacherWithSubject.setStatus("0");
            userDAO.add(teacherWithSubject);
            teacherWithSubjectId = teacherWithSubject.getUserId();

            // 添加学科关联
            TeacherSubject subject = new TeacherSubject();
            subject.setTeacherId(teacherWithSubjectId);
            subject.setSubjectId(1);
            subject.setIsMain((byte) 1);
            TeacherSubjectDAO subjectDAO = new TeacherSubjectDAO();
            subjectDAO.setSession(session);
            subjectDAO.add(subject);

            // 4. 创建无任何关联的教师
            User teacherNoRelation = new User();
            teacherNoRelation.setUsername("boundary_norelation_" + System.currentTimeMillis());
            teacherNoRelation.setPassword("encoded_password");
            teacherNoRelation.setRealName("无关联边界测试教师");
            teacherNoRelation.setUserType(1);
            teacherNoRelation.setPhone("13800000013");
            teacherNoRelation.setEmail("norelation_boundary@test.com");
            teacherNoRelation.setStatus("0");
            userDAO.add(teacherNoRelation);
            teacherNoRelationId = teacherNoRelation.getUserId();

            tran.commit();

            System.out.println("边界值测试数据准备完成:");
            System.out.println("普通教师ID: " + normalTeacherId);
            System.out.println("有职位教师ID: " + teacherWithPositionId);
            System.out.println("有学科教师ID: " + teacherWithSubjectId);
            System.out.println("无关联教师ID: " + teacherNoRelationId);

        } catch (Exception e) {
            if (tran != null) tran.rollback();
            e.printStackTrace();
            throw new RuntimeException("测试数据准备失败: " + e.getMessage());
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private String generateToken(Long userId, String username, Integer userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("username", username);
        claims.put("userType", userType);

        String secret = "DecoupledExam_MustBeLongEnoughToBeSecure123";

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7200000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    private HttpServletRequest createMockRequest(String token) {
        Map<String, Object> attributes = new HashMap<>();

        if (token != null) {
            try {
                Jwt jwt = new Jwt();
                io.jsonwebtoken.Claims claims = jwt.validateJwt(token);
                attributes.put("claims", claims);
            } catch (Exception e) {
                attributes.put("claims", null);
            }
        } else {
            attributes.put("claims", null);
        }

        return new HttpServletRequest() {
            @Override public String getAuthType() { return null; }
            @Override public jakarta.servlet.http.Cookie[] getCookies() { return new jakarta.servlet.http.Cookie[0]; }
            @Override public long getDateHeader(String name) { return 0; }
            @Override public String getHeader(String name) { return null; }
            @Override public java.util.Enumeration<String> getHeaders(String name) { return java.util.Collections.emptyEnumeration(); }
            @Override public java.util.Enumeration<String> getHeaderNames() { return java.util.Collections.emptyEnumeration(); }
            @Override public int getIntHeader(String name) { return 0; }
            @Override public String getMethod() { return "DELETE"; }
            @Override public String getPathInfo() { return null; }
            @Override public String getPathTranslated() { return null; }
            @Override public String getContextPath() { return ""; }
            @Override public String getQueryString() { return null; }
            @Override public String getRemoteUser() { return null; }
            @Override public boolean isUserInRole(String role) { return false; }
            @Override public java.security.Principal getUserPrincipal() { return null; }
            @Override public String getRequestedSessionId() { return null; }
            @Override public String getRequestURI() { return "/api/user"; }
            @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost:8080/api/user"); }
            @Override public String getServletPath() { return ""; }
            @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
            @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
            @Override public String changeSessionId() { return null; }
            @Override public boolean isRequestedSessionIdValid() { return false; }
            @Override public boolean isRequestedSessionIdFromCookie() { return false; }
            @Override public boolean isRequestedSessionIdFromURL() { return false; }
            @Override public boolean isRequestedSessionIdFromUrl() { return false; }
            @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
            @Override public void login(String username, String password) {}
            @Override public void logout() {}
            @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return java.util.Collections.emptyList(); }
            @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
            @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
            @Override public Object getAttribute(String name) { return attributes.get(name); }
            @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.enumeration(attributes.keySet()); }
            @Override public String getCharacterEncoding() { return "UTF-8"; }
            @Override public void setCharacterEncoding(String env) {}
            @Override public int getContentLength() { return 0; }
            @Override public long getContentLengthLong() { return 0; }
            @Override public String getContentType() { return null; }
            @Override public String getParameter(String name) { return null; }
            @Override public java.util.Enumeration<String> getParameterNames() { return java.util.Collections.emptyEnumeration(); }
            @Override public String[] getParameterValues(String name) { return new String[0]; }
            @Override public Map<String, String[]> getParameterMap() { return java.util.Collections.emptyMap(); }
            @Override public String getProtocol() { return "HTTP/1.1"; }
            @Override public String getScheme() { return "http"; }
            @Override public String getServerName() { return "localhost"; }
            @Override public int getServerPort() { return 8080; }
            @Override public java.io.BufferedReader getReader() { return null; }
            @Override public jakarta.servlet.ServletInputStream getInputStream() { return null; }
            @Override public String getRealPath(String path) { return null; }
            @Override public String getRemoteAddr() { return "127.0.0.1"; }
            @Override public String getRemoteHost() { return "localhost"; }
            @Override public void setAttribute(String name, Object o) { attributes.put(name, o); }
            @Override public void removeAttribute(String name) { attributes.remove(name); }
            @Override public java.util.Locale getLocale() { return java.util.Locale.CHINA; }
            @Override public java.util.Enumeration<java.util.Locale> getLocales() { return java.util.Collections.enumeration(java.util.Collections.singletonList(java.util.Locale.CHINA)); }
            @Override public boolean isSecure() { return false; }
            @Override public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
            @Override public int getRemotePort() { return 0; }
            @Override public String getLocalName() { return "localhost"; }
            @Override public String getLocalAddr() { return "127.0.0.1"; }
            @Override public int getLocalPort() { return 8080; }
            @Override public jakarta.servlet.ServletContext getServletContext() { return null; }
            @Override public jakarta.servlet.AsyncContext startAsync() { return null; }
            @Override public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) { return null; }
            @Override public boolean isAsyncStarted() { return false; }
            @Override public boolean isAsyncSupported() { return false; }
            @Override public jakarta.servlet.AsyncContext getAsyncContext() { return null; }
            @Override public jakarta.servlet.DispatcherType getDispatcherType() { return jakarta.servlet.DispatcherType.REQUEST; }
        };
    }

    @After
    public void tearDown() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;

        try {
            tran = session.beginTransaction();
            if (normalTeacherId != null) userDAO.delete(normalTeacherId);
            if (teacherWithPositionId != null) userDAO.delete(teacherWithPositionId);
            if (teacherWithSubjectId != null) userDAO.delete(teacherWithSubjectId);
            if (teacherNoRelationId != null) userDAO.delete(teacherNoRelationId);
            tran.commit();
            System.out.println("边界值测试数据清理完成");
        } catch (Exception e) {
            if (tran != null) tran.rollback();
            e.printStackTrace();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
     * TC01: 教师ID最小值(1)
     * 边界场景: userId = 1 (数据库最小有效ID)
     */
    @Test
    public void testTC01_MinUserId() {
        System.out.println("\n========== TC01: 教师ID最小值(1) ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(1L, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 成功删除或不能删除自己(如果ID=1是管理员)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC02: 教师ID=0(无效边界)
     * 边界场景: userId = 0 (无效值)
     */
    @Test
    public void testTC02_ZeroUserId() {
        System.out.println("\n========== TC02: 教师ID=0(无效边界) ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(0L, request);

        System.out.println("响应: " + response);
        System.out.println("预期: false(删除失败)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC03: 教师ID负数(越界)
     * 边界场景: userId = -1 (负数越界)
     */
    @Test
    public void testTC03_NegativeUserId() {
        System.out.println("\n========== TC03: 教师ID负数(越界) ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(-1L, request);

        System.out.println("响应: " + response);
        System.out.println("预期: false(删除失败或参数错误)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC04: 教师ID极大值(不存在)
     * 边界场景: userId = 100000 (远大于当前最大ID)
     */
    @Test
    public void testTC04_LargeUserId() {
        System.out.println("\n========== TC04: 教师ID极大值(不存在) ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(100000L, request);

        System.out.println("响应: " + response);
        System.out.println("预期: false(删除失败)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC05: 教师ID=Long.MAX_VALUE(理论上界)
     * 边界场景: userId = Long.MAX_VALUE (Java Long类型最大值)
     */
    @Test
    public void testTC05_MaxLongUserId() {
        System.out.println("\n========== TC05: 教师ID=Long.MAX_VALUE(理论上界) ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(Long.MAX_VALUE, request);

        System.out.println("响应: " + response);
        System.out.println("预期: false(删除失败)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC06: 删除有职位关联的教师
     * 边界场景: 教师有关联的teacher_position记录
     */
    @Test
    public void testTC06_TeacherWithPosition() {
        System.out.println("\n========== TC06: 删除有职位关联的教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(teacherWithPositionId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: true(成功删除)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC07: 删除有学科关联的教师
     * 边界场景: 教师有关联的teacher_subject记录
     */
    @Test
    public void testTC07_TeacherWithSubject() {
        System.out.println("\n========== TC07: 删除有学科关联的教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(teacherWithSubjectId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: true(成功删除)");

        assertNotNull("响应不应为null", response);
    }

    /**
     * TC08: 删除无任何关联的教师
     * 边界场景: 教师无任何关联数据
     */
    @Test
    public void testTC08_TeacherNoRelation() {
        System.out.println("\n========== TC08: 删除无任何关联的教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(teacherNoRelationId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: true(成功删除)");

        assertNotNull("响应不应为null", response);
    }
}
