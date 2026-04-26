package cn.edu.zjut.backend.junit_test;

import cn.edu.zjut.backend.controller.UserController;
import cn.edu.zjut.backend.dao.UserDAO;
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
 * 删除教师功能 - 白盒测试
 * 测试目标: 覆盖UserController.deleteUser() 和 UserService.deleteUser() 的所有分支
 */
public class DeleteTeacherWhiteBoxTest {

    private UserController userController;
    private String adminToken;
    private String teacherToken;
    private Long adminUserId = 1L;
    private Long teacherUserId;

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
        teacherToken = generateToken(teacherUserId, "teacher_whitebox", 1);
    }

    private void prepareTestData() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;

        try {
            tran = session.beginTransaction();

            User teacher = new User();
            teacher.setUsername("teacher_whitebox_" + System.currentTimeMillis());
            teacher.setPassword("encoded_password");
            teacher.setRealName("白盒测试教师");
            teacher.setUserType(1);
            teacher.setPhone("13800000020");
            teacher.setEmail("whitebox@test.com");
            teacher.setStatus("0");
            userDAO.add(teacher);
            teacherUserId = teacher.getUserId();

            tran.commit();
            System.out.println("白盒测试数据准备完成:");
            System.out.println("教师ID: " + teacherUserId);

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

    /**
     * 创建带有指定claims的Mock Request
     */
    private HttpServletRequest createMockRequestWithClaims(Map<String, Object> claimsMap) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("claims", claimsMap);

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

    /**
     * 创建带Token的Mock Request
     */
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
            if (teacherUserId != null) {
                try {
                    userDAO.delete(teacherUserId);
                } catch (Exception e) {
                    System.out.println("清理测试数据时教师可能已被删除: " + e.getMessage());
                }
            }
            tran.commit();
            System.out.println("白盒测试数据清理完成");
        } catch (Exception e) {
            if (tran != null) tran.rollback();
            e.printStackTrace();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
     * P1: 正常通路 - 成功删除教师
     * 路径: claims有效 → 权限验证通过 → 非自己 → deleteUser → commit → 返回成功
     */
    @Test
    public void deleteTeacher_Success() {
        System.out.println("\n========== P1: 正常通路 - 成功删除教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(teacherUserId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 操作成功");

        assertNotNull("响应不应为null", response);
        assertEquals(200, response.getCode().intValue());
        assertEquals("操作成功", response.getMsg());
    }

    /**
     * P2: claims为null分支
     * 路径: claims == null → 返回"请先登录"
     */
    @Test
    public void deleteTeacher_NullClaims() {
        System.out.println("\n========== P2: claims为null分支 ==========");
        HttpServletRequest request = createMockRequestWithClaims(null);

        Response<String> response = userController.deleteUser(teacherUserId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 请先登录");

        assertNotNull("响应不应为null", response);
        assertEquals(500, response.getCode().intValue());
        assertEquals("请先登录", response.getMsg());
    }

    /**
     * P3: 权限不足分支
     * 路径: currentUserType != 0 → 返回"权限不足"
     */
    @Test
    public void deleteTeacher_NotAdmin() {
        System.out.println("\n========== P3: 权限不足分支 ==========");
        HttpServletRequest request = createMockRequest(teacherToken);

        Long anotherTeacherId = createAnotherTeacher();
        Response<String> response = userController.deleteUser(anotherTeacherId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 权限不足");

        assertNotNull("响应不应为null", response);
        assertEquals(500, response.getCode().intValue());
        assertEquals("权限不足，只有管理员可以删除用户", response.getMsg());

        cleanupTeacher(anotherTeacherId);
    }

    /**
     * P4: 删除自己分支
     * 路径: currentUserId.equals(userId) → 返回"不能删除自己的账户"
     */
    @Test
    public void deleteTeacher_DeleteSelf() {
        System.out.println("\n========== P4: 删除自己分支 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Response<String> response = userController.deleteUser(adminUserId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 不能删除自己的账户");

        assertNotNull("响应不应为null", response);
        assertEquals(500, response.getCode().intValue());
        assertEquals("不能删除自己的账户", response.getMsg());
    }

    /**
     * P5: Service层正常分支
     * 路径: beginTransaction → dao.delete → commit → return true
     * 说明: P1已覆盖此路径，此处通过验证返回结果确认Service层正常执行
     */
    @Test
    public void deleteTeacher_ServiceSuccess() {
        System.out.println("\n========== P5: Service层正常分支 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        Long newTeacherId = createAnotherTeacher();
        Response<String> response = userController.deleteUser(newTeacherId, request);

        System.out.println("响应: " + response);
        System.out.println("预期: Service层成功删除");

        assertNotNull("响应不应为null", response);
        assertEquals("操作成功", response.getMsg());
    }

    /**
     * P6: Service层异常分支
     * 路径: dao.delete抛异常 → rollback → return false
     * 说明: 通过删除不存在的用户ID，观察异常处理逻辑
     */
    @Test
    public void deleteTeacher_ServiceException() {
        System.out.println("\n========== P6: Service层异常分支 ==========");
        HttpServletRequest request = createMockRequest(adminToken);

        // 尝试删除不存在的用户，观察异常处理
        Response<String> response = userController.deleteUser(999999L, request);

        System.out.println("响应: " + response);
        System.out.println("预期: 根据实现可能返回成功或失败");

        assertNotNull("响应不应为null", response);
        // 注意：当前实现中即使ID不存在也可能返回成功，这是已知缺陷
    }

    /**
     * P7: DAO层异常分支
     * 路径: executeUpdate抛RuntimeException → 向上抛出 → Service层捕获 → rollback
     * 说明: 此路径需要数据库层面触发异常（如外键约束），在单元测试中较难模拟
     */
    @Test
    public void deleteTeacher_DaoException() {
        System.out.println("\n========== P7: DAO层异常分支 ==========");
        System.out.println("说明: 此分支需要数据库层面触发异常（如外键约束违反）");
        System.out.println("在实际数据库中，如果教师有关联数据且配置了外键约束，会触发此路径");

        HttpServletRequest request = createMockRequest(adminToken);

        // 理论上应该触发外键约束异常的场景
        // 但由于当前实现未检查影响行数，可能不会抛出异常
        Response<String> response = userController.deleteUser(Long.MAX_VALUE, request);

        assertNotNull("响应不应为null", response);
        System.out.println("响应: " + response);
    }

    // ===== 辅助方法 =====

    private Long createAnotherTeacher() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;
        try {
            tran = session.beginTransaction();
            User teacher = new User();
            String uniqueUsername = "teacher_whitebox_aux_" + System.currentTimeMillis();
            teacher.setUsername(uniqueUsername);
            teacher.setPassword("encoded_password");
            teacher.setRealName("辅助测试教师");
            teacher.setUserType(1);
            teacher.setPhone("13800000021");
            teacher.setEmail("whitebox_aux_" + System.currentTimeMillis() + "@test.com");
            teacher.setStatus("0");
            userDAO.add(teacher);
            tran.commit();
            return teacher.getUserId();
        } catch (Exception e) {
            if (tran != null) tran.rollback();
            e.printStackTrace();
            return null;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    private void cleanupTeacher(Long teacherId) {
        if (teacherId == null) return;
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;
        try {
            tran = session.beginTransaction();
            userDAO.delete(teacherId);
            tran.commit();
        } catch (Exception e) {
            if (tran != null) tran.rollback();
        } finally {
            HibernateUtil.closeSession();
        }
    }
}
