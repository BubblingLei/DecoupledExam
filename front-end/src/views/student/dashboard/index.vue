<template>
  <div class="max-w-6xl mx-auto px-8">
    <!-- 顶部个人信息 + 概览 -->
    <div class="flex flex-col md:flex-row items-stretch gap-6 mb-8">
      <div class="flex-1 bg-base-100 p-6 rounded-lg shadow flex items-center gap-6">
          <div class="avatar">
            <div class="w-24 rounded-full ring ring-primary ring-offset-base-100 ring-offset-2">
              <img :src="avatarUrl || defaultAvatar" alt="头像" />
            </div>
          </div>
        <div class="flex-1">
          <h2 class="text-2xl font-semibold">{{ userProfile.realName || userProfile.username }}</h2>
          <p class="text-sm text-gray-500">{{ userProfile.username }} · {{ getUserTypeText(userProfile.userType) }}</p>
          <div class="mt-4 grid grid-cols-3 gap-4">
            <div class="p-3 bg-primary text-primary-content rounded-lg">
              <div class="text-sm">考试次数</div>
              <div class="text-lg font-bold">{{ examCount }}</div>
            </div>
            <div class="p-3 bg-secondary text-secondary-content rounded-lg">
              <div class="text-sm">课程数量</div>
              <div class="text-lg font-bold">{{ courseCount }}</div>
            </div>
            <div class="p-3 bg-accent text-accent-content rounded-lg">
              <div class="text-sm">错题数量</div>
              <div class="text-lg font-bold">{{ mistakeCount }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="w-full md:w-1/2 bg-base-100 p-6 rounded-lg shadow">
        <h3 class="text-lg font-semibold mb-3">成绩分布</h3>
        <div class="h-40 flex items-center justify-center">
          <div class="text-sm text-gray-400">图表占位</div>
        </div>
      </div>
    </div>

    <!-- 三大功能入口 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
      <router-link to="/student/courses" class="flex flex-col items-center justify-center p-3 rounded-lg shadow bg-primary text-primary-content" style="aspect-ratio: 7/3;">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
        <div class="text-xl font-medium">课程</div>
      </router-link>

      <router-link to="/student/mistakes" class="flex flex-col items-center justify-center p-3 rounded-lg shadow bg-secondary text-secondary-content" style="aspect-ratio: 7/3;">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M9 20h6a2 2 0 002-2v-5l-3-3-3 3v5a2 2 0 01-2 2z" />
        </svg>
        <div class="text-xl font-medium">错题</div>
      </router-link>

      <router-link to="/student/grades" class="flex flex-col items-center justify-center p-3 rounded-lg shadow bg-accent text-accent-content" style="aspect-ratio: 7/3;">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 17a1 1 0 010-2h2a1 1 0 010 2h-2zm-6-6a9 9 0 1118 0v6a2 2 0 01-2 2H7a2 2 0 01-2-2v-6z" />
        </svg>
        <div class="text-xl font-medium">成绩</div>
      </router-link>
    </div>

    <!-- 最近考试 -->
    <div class="flex gap-6 mb-8">
      <router-link to="/exam" class="w-36 flex-shrink-0 flex flex-col items-center justify-center p-6 rounded-lg shadow bg-warning text-warning-content">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
        </svg>
        <div class="text-lg font-medium">考试</div>
      </router-link>

      <div class="flex-1 bg-base-100 p-6 rounded-lg shadow">
        <h2 class="text-xl font-semibold mb-4">最近考试</h2>
        <ul class="space-y-3">
          <li class="flex justify-between items-center p-4 bg-base-200 rounded">
            <div>
              <div class="font-medium">期末模拟考试</div>
              <div class="text-sm text-gray-500">2025-12-28 · 数学</div>
            </div>
            <div class="badge badge-outline">未开始</div>
          </li>
          <li class="flex justify-between items-center p-4 bg-base-200 rounded">
            <div>
              <div class="font-medium">章节测验</div>
              <div class="text-sm text-gray-500">2025-12-20 · 程序设计</div>
            </div>
            <div class="badge badge-success">已结束</div>
          </li>
        </ul>
      </div>
    </div>

    <!-- 最近通知 -->
    <div class="flex gap-6">
      <router-link to="/student/notifications" class="w-36 flex-shrink-0 flex flex-col items-center justify-center p-6 rounded-lg shadow bg-secondary text-secondary-content">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8m0 0v8a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm0 0L12 13" />
        </svg>
        <div class="text-lg font-medium">通知</div>
      </router-link>

      <div class="flex-1 bg-base-100 p-6 rounded-lg shadow">
        <h2 class="text-xl font-semibold mb-4">最近通知</h2>
        <div class="space-y-3">
          <div class="p-3 bg-base-200 rounded">教务处：期末考试安排已公布，请注意查收。</div>
          <div class="p-3 bg-base-200 rounded">老师：作业已批改，请在成绩页查看。</div>
          <div class="p-3 bg-base-200 rounded">系统：请及时补充个人信息。</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getUserInfoByIdAPI } from '../../../apis/Server/userAPI';
import { jwtDecode } from 'jwt-decode';

const userProfile = ref<any>({
  id: 0,
  username: '',
  realName: '',
  phone: '',
  userType: 2,
  avatar: ''
});

const avatarUrl = ref<string>('');
const defaultAvatar = 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg';

// 三个统计项（可以后续改为从接口获取）
const examCount = ref<number>(12);
const courseCount = ref<number>(8);
const mistakeCount = ref<number>(24);

const ensureFullUrl = (path: string) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${import.meta.env.VITE_RESOURCES_URL}${path}`;
}

const getUserTypeText = (userType: number) => {
  switch (userType) {
    case 0: return '管理员';
    case 1: return '教师';
    case 2: return '学生';
    default: return '未知';
  }
}

onMounted(async () => {
  try {
    const token = localStorage.getItem('token');
    if (!token) return;
    // 从 token 中解析用户 id（与个人信息页面一致）
    const decoded: any = jwtDecode(token);
    const userId = decoded.id;

    const res: any = await getUserInfoByIdAPI(userId, token);
    if (res && res.code === 200 && res.data) {
      const data = res.data;
      userProfile.value = {
        id: data.userId || data.id,
        username: data.username,
        realName: data.realName,
        phone: data.phone,
        userType: data.userType,
        avatar: data.avatarUrl || data.avatar
      };

      if (userProfile.value.avatar) {
        avatarUrl.value = ensureFullUrl(userProfile.value.avatar);
      }
    }
  } catch (err) {
    console.error('获取用户信息失败', err);
  }
});
</script>