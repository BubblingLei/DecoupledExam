package cn.edu.zjut.backend.junit_test;

import cn.edu.zjut.backend.controller.UserController;
import cn.edu.zjut.backend.dao.UserDAO;
import cn.edu.zjut.backend.po.User;
import cn.edu.zjut.backend.util.HibernateUtil;
import cn.edu.zjut.backend.util.Jwt;
import cn.edu.zjut.backend.util.Response;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 删除教师功能 - 等价类测试
 * 测试目标: UserController.deleteUser() 方法
 */
public class DeleteTeacherEquivalenceClassTest {

    private UserController userController;
    private String adminToken;
    private String teacherToken;
    private String studentToken;
    private Long adminUserId = 1L;
    private Long teacherUserId;
    private Long studentUserId;
    private Long disabledTeacherUserId;

    @Before
    public void setUp() {
        userController = new UserController();
        // 手动注入依赖（因为没有使用Spring容器）
        cn.edu.zjut.backend.service.UserService userService = new cn.edu.zjut.backend.service.UserService();
        try {
            java.lang.reflect.Field field = UserController.class.getDeclaredField("userService");
            field.setAccessible(true);
            field.set(userController, userService);
        } catch (Exception e) {
            e.printStackTrace();
        }
        prepareTestData();
        adminToken = generateToken(adminUserId, "admin", 0);
        teacherToken = generateToken(teacherUserId, "teacher_test", 1);
        studentToken = generateToken(studentUserId, "student_test", 2);
    }

    private void prepareTestData() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;

        try {
            tran = session.beginTransaction();

            User teacher = new User();
            teacher.setUsername("teacher_eq_test");
            teacher.setPassword("encoded_password");
            teacher.setRealName("等价类测试教师");
            teacher.setUserType(1);
            teacher.setPhone("13800000001");
            teacher.setEmail("teacher_eq@test.com");
            teacher.setStatus("0");
            userDAO.add(teacher);

            User disabledTeacher = new User();
            disabledTeacher.setUsername("teacher_disabled_test");
            disabledTeacher.setPassword("encoded_password");
            disabledTeacher.setRealName("停用测试教师");
            disabledTeacher.setUserType(1);
            disabledTeacher.setPhone("13800000002");
            disabledTeacher.setEmail("teacher_disabled@test.com");
            disabledTeacher.setStatus("1");
            userDAO.add(disabledTeacher);

            User student = new User();
            student.setUsername("student_eq_test");
            student.setPassword("encoded_password");
            student.setRealName("等价类测试学生");
            student.setUserType(2);
            student.setPhone("13800000003");
            student.setEmail("student_eq@test.com");
            student.setStatus("0");
            userDAO.add(student);

            tran.commit();

            teacherUserId = teacher.getUserId();
            disabledTeacherUserId = disabledTeacher.getUserId();
            studentUserId = student.getUserId();

            System.out.println("测试数据准备完成:");
            System.out.println("教师ID: " + teacherUserId);
            System.out.println("停用教师ID: " + disabledTeacherUserId);
            System.out.println("学生ID: " + studentUserId);

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

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.HS256, "DecoupledExam_MustBeLongEnoughToBeSecure123")
                .compact();
    }

    /**
     * 使用Mockito创建Mock HttpServletRequest - 最优雅的方式
     */
    private HttpServletRequest createMockRequest(String token) {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);

        if (token != null) {
            try {
                Jwt jwt = new Jwt();
                Claims claims = jwt.validateJwt(token);
                when(mockRequest.getAttribute("claims")).thenReturn(claims);
            } catch (Exception e) {
                when(mockRequest.getAttribute("claims")).thenReturn(null);
            }
        } else {
            when(mockRequest.getAttribute("claims")).thenReturn(null);
        }

        return mockRequest;
    }

    @After
    public void tearDown() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;

        try {
            tran = session.beginTransaction();
            if (teacherUserId != null) userDAO.delete(teacherUserId);
            if (disabledTeacherUserId != null) userDAO.delete(disabledTeacherUserId);
            if (studentUserId != null) userDAO.delete(studentUserId);
            tran.commit();
            System.out.println("测试数据清理完成");
        } catch (Exception e) {
            if (tran != null) tran.rollback();
            e.printStackTrace();
        } finally {
            HibernateUtil.closeSession();
        }
    }

    @Test
    public void testTC1_AdminDeleteNormalTeacher() {
        System.out.println("\n========== TC1: 管理员成功删除正常教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);
        Response<String> response = userController.deleteUser(teacherUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: True (成功) - 操作成功");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回成功消息", "操作成功", response.getMsg());
    }

    @Test
    public void testTC2_UnauthorizedDelete() {
        System.out.println("\n========== TC2: 未登录尝试删除教师 ==========");
        HttpServletRequest request = createMockRequest(null);
        Response<String> response = userController.deleteUser(teacherUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: False (失败) - 请先登录");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回未登录错误", "请先登录", response.getMsg());
    }

    @Test
    public void testTC3_TeacherDeleteOtherTeacher() {
        System.out.println("\n========== TC3: 教师尝试删除其他教师 ==========");
        HttpServletRequest request = createMockRequest(teacherToken);
        Long anotherTeacherId = createAnotherTeacher();
        Response<String> response = userController.deleteUser(anotherTeacherId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: False (失败) - 权限不足");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回权限不足错误", "权限不足，只有管理员可以删除用户", response.getMsg());
        cleanupTeacher(anotherTeacherId);
    }

    @Test
    public void testTC4_StudentDeleteTeacher() {
        System.out.println("\n========== TC4: 学生尝试删除教师 ==========");
        HttpServletRequest request = createMockRequest(studentToken);
        Response<String> response = userController.deleteUser(teacherUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: False (失败) - 权限不足");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回权限不足错误", "权限不足，只有管理员可以删除用户", response.getMsg());
    }

    @Test
    public void testTC5_DeleteNonExistentTeacher() {
        System.out.println("\n========== TC5: 删除不存在的教师ID ==========");
        HttpServletRequest request = createMockRequest(adminToken);
        Long nonExistentId = 99999L;
        Response<String> response = userController.deleteUser(nonExistentId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: False (失败) - 用户删除失败");
        assertNotNull("响应不应为null", response);
    }

    @Test
    public void testTC6_AdminDeleteStudent() {
        System.out.println("\n========== TC6: 管理员删除学生账号 ==========");
        HttpServletRequest request = createMockRequest(adminToken);
        Response<String> response = userController.deleteUser(studentUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: True (成功) - 当前代码允许删除任何用户");
        assertNotNull("响应不应为null", response);
    }

    @Test
    public void testTC7_AdminDeleteSelf() {
        System.out.println("\n========== TC7: 管理员删除自己的账号 ==========");
        HttpServletRequest request = createMockRequest(adminToken);
        Response<String> response = userController.deleteUser(adminUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: False (失败) - 不能删除自己的账户");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回不能删除自己的错误", "不能删除自己的账户", response.getMsg());
    }

    @Test
    public void testTC8_DeleteDisabledTeacher() {
        System.out.println("\n========== TC8: 删除已停用的教师 ==========");
        HttpServletRequest request = createMockRequest(adminToken);
        Response<String> response = userController.deleteUser(disabledTeacherUserId, request);
        System.out.println("响应: " + response);
        System.out.println("预期: True (成功) - 操作成功");
        assertNotNull("响应不应为null", response);
        assertEquals("应返回成功消息", "操作成功", response.getMsg());
    }

    private Long createAnotherTeacher() {
        Session session = HibernateUtil.getSession();
        UserDAO userDAO = new UserDAO();
        userDAO.setSession(session);
        Transaction tran = null;
        try {
            tran = session.beginTransaction();
            User teacher = new User();
            String uniqueUsername = "teacher_another_test_" + System.currentTimeMillis();
            teacher.setUsername(uniqueUsername);
            teacher.setPassword("encoded_password");
            teacher.setRealName("另一个测试教师");
            teacher.setUserType(1);
            teacher.setPhone("13800000004");
            teacher.setEmail("teacher_another_" + System.currentTimeMillis() + "@test.com");
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
