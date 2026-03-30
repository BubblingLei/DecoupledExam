<template>
  <div class="max-w-6xl mx-auto px-8 py-6">
    <!-- 个人信息区域 -->
    <div class="flex flex-col md:flex-row items-stretch gap-6 mb-8">
      <div class="flex-1 bg-base-100 p-6 rounded-lg shadow flex items-center gap-6">
        <div class="avatar">
          <div class="w-24 rounded-full ring ring-primary ring-offset-base-100 ring-offset-2">
            <img :src="avatarUrl || defaultAvatar" alt="头像" />
          </div>
        </div>
        <div class="flex-1">
          <h2 class="text-2xl font-semibold">{{ profile.realName || profile.username }}</h2>
          <p class="text-sm text-gray-500">{{ profile.username }} · 教师 · {{ positionText }}</p>
          <div class="mt-4 grid grid-cols-3 gap-4">
            <router-link to="/teacher/courses" class="p-3 bg-primary text-primary-content rounded-lg">
              <div class="text-sm">课程管理</div>
              <div class="text-lg font-bold">管理</div>
            </router-link>
            <router-link to="/exam" class="p-3 bg-secondary text-secondary-content rounded-lg">
              <div class="text-sm">我的考试</div>
              <div class="text-lg font-bold">考试</div>
            </router-link>
            <div class="p-3 bg-accent text-accent-content rounded-lg">
              <div class="text-sm">通知</div>
              <div class="text-lg font-bold">通知</div>
            </div>
          </div>
        </div>
      </div>

      <div class="w-full md:w-1/2 bg-base-100 p-6 rounded-lg shadow">
        <h3 class="text-lg font-semibold mb-3">快速入口</h3>
        <div class="grid grid-cols-2 gap-3">
          <router-link class="p-3 rounded-lg bg-base-200 flex items-center justify-center" to="/teacher/courses">
            创建课程
          </router-link>
          <router-link class="p-3 rounded-lg bg-base-200 flex items-center justify-center" to="/teacher/courses">
            删除/修改课程
          </router-link>
          <router-link class="p-3 rounded-lg bg-base-200 flex items-center justify-center" to="/exam">
            考试管理
          </router-link>
          <router-link class="p-3 rounded-lg bg-base-200 flex items-center justify-center" to="/teacher/question">
            题库管理
          </router-link>
        </div>
      </div>
    </div>

    <!-- 功能卡片 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <router-link to="/teacher/courses" class="bg-base-100 p-6 rounded-lg shadow">
          <h4 class="text-lg font-medium mb-3">课程管理</h4>
          <p class="text-sm text-gray-500">创建/删除/修改课程</p>
          <div class="mt-4 flex gap-2">
            <button class="btn btn-primary">管理课程</button>
          </div>
        </router-link>

        <router-link to="/exam" class="bg-base-100 p-6 rounded-lg shadow">
          <h4 class="text-lg font-medium mb-3">考试管理</h4>
          <p class="text-sm text-gray-500">发布考试、设置考试</p>
          <div class="mt-4 flex gap-2">
            <button class="btn btn-secondary">考试管理</button>
          </div>
        </router-link>

        <div class="bg-base-100 p-6 rounded-lg shadow">
          <h4 class="text-lg font-medium mb-3">通知管理</h4>
          <p class="text-sm text-gray-500">发布/接受通知</p>
          <div class="mt-4">
            <button class="btn btn-accent">发布通知</button>
          </div>
        </div>

        <router-link to="/teacher/subject-binding" class="bg-base-100 p-6 rounded-lg shadow">
          <h4 class="text-lg font-medium mb-3">学科绑定</h4>
          <p class="text-sm text-gray-500">绑定/管理教学学科</p>
          <div class="mt-4">
            <button class="btn btn-primary">绑定学科</button>
          </div>
        </router-link>

    </div>

    <!-- 题库与试卷 -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <router-link to="/teacher/question" class="bg-base-100 p-6 rounded-lg shadow">
        <h4 class="text-lg font-medium mb-3">题库管理</h4>
        <p class="text-sm text-gray-500">创建/删除/修改题目</p>
        <div class="mt-4">
          <button class="btn">创建题目</button>
        </div>
      </router-link>

      <router-link to="/teacher/exam-paper" class="bg-base-100 p-6 rounded-lg shadow">
        <h4 class="text-lg font-medium mb-3">试卷管理</h4>
        <p class="text-sm text-gray-500">组卷 / 发布试卷</p>
        <div class="mt-4">
          <button class="btn">创建试卷</button>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { getAvatarAPI, getUserInfoByIdAPI, getTeacherPositionAPI } from '../../../apis/Server/userAPI';
import { jwtDecode } from 'jwt-decode';

const profile = ref<any>({ username: '', realName: '', avatar: '' });
const avatarUrl = ref('');
const defaultAvatar = 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg';
const positionRole = ref<number | null>(null);
const teacherId = ref<number | null>(null);

const positionText = computed(() => {
  if (positionRole.value === 0) return '任课老师';
  if (positionRole.value === 1) return '教务老师';
  return '未知职位';
});

onMounted(async () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return;
    const decoded: any = jwtDecode(token);
    const userId = decoded.id;
    // 从JWT token中获取教师ID，优先使用id字段作为教师ID
    teacherId.value = userId;

    // 优先通过用户ID接口获取完整用户信息
    try {
      const userRes: any = await getUserInfoByIdAPI(userId, token);
      if (userRes && userRes.code === 200 && userRes.data) {
        const userData = userRes.data;
        profile.value = {
          username: userData.username || '',
          realName: userData.realName || userData.username || '',
          avatar: userData.avatarUrl || userData.avatar || ''
        };
        if (profile.value.avatar) {
          avatarUrl.value = ensureFullUrl(profile.value.avatar);
        } else {
          // 尝试单独获取头像
          await loadAvatar(token);
        }
      } else {
        // Fallback: 从 token 中填充
        profile.value = {
          username: decoded.username || '',
          realName: decoded.realName || decoded.username || '',
          avatar: decoded.faceImg || ''
        };
        if (profile.value.avatar) avatarUrl.value = ensureFullUrl(profile.value.avatar);
        else await loadAvatar(token);
      }
    } catch (err) {
      profile.value = {
        username: decoded.username || '',
        realName: decoded.realName || decoded.username || '',
        avatar: decoded.faceImg || ''
      };
      if (profile.value.avatar) avatarUrl.value = ensureFullUrl(profile.value.avatar);
      else await loadAvatar(token);
    }

    // 获取教师职位
    console.log('获取教师职位，教师ID:', teacherId.value);
    if (teacherId.value) {
      try {
        const posRes: any = await getTeacherPositionAPI(teacherId.value, token);
        console.log('教师职位接口响应:', posRes);
        if (posRes && posRes.code === 200 && posRes.data) {
          positionRole.value = posRes.data?.role ?? null;
          console.log('设置教师职位为:', positionRole.value);
        } else {
          console.error('获取教师职位失败，响应数据:', posRes);
        }
      } catch (err) {
        console.error('获取教师职位失败', err);
      }
    } else {
      console.error('无法获取教师ID');
    }
  } catch (err) {
    console.error('获取教师信息或职位失败', err);
  }
});

async function loadAvatar(token: string) {
  try {
    const avatarRes: any = await getAvatarAPI(token);
    if (avatarRes && avatarRes.code === 200 && avatarRes.data) {
      if (typeof avatarRes.data === 'string') {
        avatarUrl.value = ensureFullUrl(avatarRes.data);
      } else {
        // 如果返回的是二进制流或其他格式，忽略，保持默认头像
        avatarUrl.value = defaultAvatar;
      }
    } else {
      avatarUrl.value = defaultAvatar;
    }
  } catch (err) {
    avatarUrl.value = defaultAvatar;
  }
}

function ensureFullUrl(path: string) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${import.meta.env.VITE_RESOURCES_URL}${path}`;
}
</script>

<style scoped>
.avatar img { object-fit: cover; }
</style>