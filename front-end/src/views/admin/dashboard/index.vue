<template>
  <div class="max-w-6xl mx-auto px-8 py-6">
    <!-- 顶部个人信息 + 数据统计 -->
    <div class="flex flex-col md:flex-row items-stretch gap-6 mb-8">
      <div class="flex-1 bg-base-100 p-6 rounded-lg shadow flex items-center gap-6">
        <div class="avatar">
          <div class="w-24 rounded-full ring ring-primary ring-offset-base-100 ring-offset-2">
            <img :src="avatarUrl || defaultAvatar" alt="头像" />
          </div>
        </div>
        <div class="flex-1">
          <h2 class="text-2xl font-semibold">{{ adminProfile.realName || adminProfile.username }}</h2>
          <p class="text-sm text-gray-500">{{ adminProfile.username }} · 管理员</p>
          <div class="mt-4 grid grid-cols-3 gap-4">
            <router-link to="/admin/user-management" class="p-3 bg-primary text-primary-content rounded-lg">
              <div class="text-sm">用户管理</div>
              <div class="text-lg font-bold">管理</div>
            </router-link>
            <router-link to="/admin/logs" class="p-3 bg-secondary text-secondary-content rounded-lg">
              <div class="text-sm">操作日志</div>
              <div class="text-lg font-bold">查看</div>
            </router-link>
            <router-link to="/admin/simulate-login" class="p-3 bg-accent text-accent-content rounded-lg">
              <div class="text-sm">模拟登录</div>
              <div class="text-lg font-bold">登录</div>
            </router-link>
          </div>
        </div>
      </div>

      <div class="w-full md:w-1/2 bg-base-100 p-6 rounded-lg shadow">
        <h3 class="text-lg font-semibold mb-4">数据统计</h3>
        <div class="space-y-3">
          <div class="flex items-center justify-between p-3 bg-primary text-primary-content rounded-lg">
            <span class="font-semibold">总用户数</span>
            <span class="text-2xl font-bold">{{ totalUsers }}</span>
          </div>
          <div class="flex items-center justify-between p-3 bg-secondary text-secondary-content rounded-lg">
            <span class="font-semibold">学生总数</span>
            <span class="text-2xl font-bold">{{ totalStudents }}</span>
          </div>
          <div class="flex items-center justify-between p-3 bg-accent text-accent-content rounded-lg">
            <span class="font-semibold">教师总数</span>
            <span class="text-2xl font-bold">{{ totalTeachers }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <div class="bg-base-100 p-6 rounded-lg shadow">
        <h2 class="text-xl font-semibold mb-4">快速操作</h2>
        <div class="grid grid-cols-2 gap-4">
          <router-link to="/admin/user-management" class="btn btn-outline">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
            </svg>
            用户管理
          </router-link>
          <router-link to="/admin/logs" class="btn btn-outline">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            操作日志
          </router-link>
          <router-link to="/admin/simulate-login" class="btn btn-outline">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"></path>
            </svg>
            模拟登录
          </router-link>
          <router-link to="/admin/subject" class="btn btn-outline">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
            学科管理
          </router-link>
        </div>
      </div>
      
      <div class="bg-base-100 p-6 rounded-lg shadow">
        <h2 class="text-xl font-semibold mb-4">系统状态</h2>
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <span>服务器状态</span>
            <span class="badge badge-success">正常</span>
          </div>
          <div class="flex items-center justify-between">
            <span>数据库连接</span>
            <span class="badge badge-success">正常</span>
          </div>
          <div class="flex items-center justify-between">
            <span>API响应时间</span>
            <span class="badge badge-success">85ms</span>
          </div>
          <div class="flex items-center justify-between">
            <span>当前在线用户</span>
            <span class="badge badge-primary">23</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-base-100 p-6 rounded-lg shadow">
      <h2 class="text-xl font-semibold mb-4">最近活动</h2>
      <div class="overflow-x-auto">
        <table class="table">
          <thead>
            <tr>
              <th>用户</th>
              <th>操作</th>
              <th>时间</th>
              <th>IP地址</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>admin</td>
              <td>登录系统</td>
              <td>2025-12-22 16:30</td>
              <td>127.0.0.1</td>
            </tr>
            <tr>
              <td>teacher1</td>
              <td>发布新课程</td>
              <td>2025-12-22 15:45</td>
              <td>192.168.1.100</td>
            </tr>
            <tr>
              <td>student1</td>
              <td>提交作业</td>
              <td>2025-12-22 14:20</td>
              <td>10.0.0.50</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getUserInfoByIdAPI } from '../../../apis/Server/userAPI';
import { jwtDecode } from 'jwt-decode';

const adminProfile = ref<any>({ username: '', realName: '', avatar: '' });
const avatarUrl = ref('');
const defaultAvatar = 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg';

// 数据统计
const totalUsers = ref<number>(152);
const totalStudents = ref<number>(128);
const totalTeachers = ref<number>(24);

const ensureFullUrl = (path: string) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `http://localhost:81${path}`;
}

onMounted(async () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return;
    
    const decoded: any = jwtDecode(token);
    const userId = decoded.id;

    // 获取管理员完整信息
    try {
      const userRes: any = await getUserInfoByIdAPI(userId, token);
      if (userRes && userRes.code === 200 && userRes.data) {
        const userData = userRes.data;
        adminProfile.value = {
          username: userData.username || '',
          realName: userData.realName || userData.username || '',
          avatar: userData.avatarUrl || userData.avatar || ''
        };
        if (adminProfile.value.avatar) {
          avatarUrl.value = ensureFullUrl(adminProfile.value.avatar);
        }
      } else {
        // Fallback: 从 token 中填充
        adminProfile.value = {
          username: decoded.username || '',
          realName: decoded.realName || decoded.username || '',
          avatar: decoded.faceImg || ''
        };
        if (adminProfile.value.avatar) avatarUrl.value = ensureFullUrl(adminProfile.value.avatar);
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
      // Fallback: 从 token 中填充
      adminProfile.value = {
        username: decoded.username || '',
        realName: decoded.realName || decoded.username || '',
        avatar: decoded.faceImg || ''
      };
    }
  } catch (error) {
    console.error('初始化管理员信息失败:', error);
  }
});
</script>