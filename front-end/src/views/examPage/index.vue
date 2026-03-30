<template>
<div v-if="examToken==='' || examToken==null" class="flex flex-col gap-4 mt-4 items-center justify-center h-screen">
  <h1 class="text-2xl font-bold text-gray-800">
    基于教考分离的考试系统
  </h1>
  <ExamPreparation
      :exam="exam"
      @verify-face-success="verifyFaceSuccess"
  />
</div>
<div v-if="isExamStarted && !isFullScreen" class="flex flex-col gap-4 mt-4 items-center justify-center h-screen">
  <!-- 可选：标题 -->
  <h1 class="text-2xl font-bold text-gray-800 mb-8">
    基于教考分离的考试系统
  </h1>

  <!-- 大号全屏按钮 -->
  <button
      class="
      w-80 h-16
      text-xl font-semibold
      bg-blue-500 hover:bg-blue-600
      text-white
      rounded-xl
      shadow-lg hover:shadow-xl
      transform hover:-translate-y-1
      transition-all duration-200
      focus:outline-none focus:ring-4 focus:ring-blue-300
    "
      @click="()=>{ requestFullscreen(); isFullScreen=true }"
  >
    🔒 全屏开始考试
  </button>
</div>
<div v-if="examPaper && isFullScreen && isExamStarted" class="flex justify-center items-start min-h-screen p-4">

  <div class="fixed w-[20vw] left-[2.5vw]">
    <ExamCameraMonitor
      :auto-start="true"
      :interval="5"
      :attention-score="attentionScore"
      @capture="uploadInvigilationVideo"
    />
  </div>

  <main v-if="examPaper" class="exam-main w-[50vw]">
    <div class="paper-preview w-full mx-auto p-6 bg-white shadow rounded-lg">
      <div class="text-center mb-8">
        <h1 class="text-2xl font-bold">{{ examPaper['paperName'] || "暂无" }}</h1>
        <p class="text-gray-500 mt-2">总分：{{ examPaper.totalScore }} 分</p>
      </div>

      <!-- 按题型分组渲染 -->
      <div v-for="group in groupedQuestions" :key="group.typeId" class="mb-10">
        <div class="flex items-center mb-4 border-b pb-2">
          <h2 class="text-xl font-semibold text-primary">
            {{ getQuestionTypeName(group.typeId) }}
          </h2>
          <span class="ml-2 text-sm text-gray-500">（共 {{ group.items.length }} 题）</span>
        </div>

        <!-- 渲染该题型下的所有题目 -->
        <div v-for="(pq, index) in group.items" :key="pq.questionId" class="mb-6" :id="pq.questionId">
          <div class="flex">
            <span class="mr-2 font-mono text-gray-700">{{ index + 1 }}.</span>
            <div class="flex-1">
              <!-- 动态组件：根据 typeId 加载对应视图 -->
              <component
                  :is="getQuestionComponent(group.typeId)"
                  :question="pq.question"
                  :showSolution="true"
                  :score="pq.score"
                  :readonly="false"
                  v-model="answers[pq.questionId]"
              />
            </div>
          </div>
        </div>
      </div>

      <div v-if="!hasQuestions" class="text-center py-10 text-gray-500">
        试卷暂无题目
      </div>
    </div>
  </main>

  <!-- 右侧导航栏 -->
  <div class="answer-panel fixed right-[2.5vw] min-w-[20vw] max-w-[20vw] max-h-[90vh] border-l border-gray-200 p-3 flex flex-col gap-4 overflow-y-auto bg-gray-50">
    <div class="text-xl font-bold">
      试卷导航面板
    </div>
    <!-- 每个题型的导航组 -->
    <div
        v-for="(group, groupIndex) in groupedQuestions"
        :key="group.typeId"
        class="border border-gray-300 rounded p-2 bg-white"
    >
      <!-- 题型标题 -->
      <div class="text-base font-bold text-gray-600 mb-1 px-1">
        {{ getQuestionTypeName(group.typeId) }}
      </div>

      <!-- 题目按钮 -->
      <div
          v-for="(pq, idx) in group.items"
          :key="pq.questionId"
          @click="scrollToQuestion(pq.questionId)"
          class="inline-flex items-center justify-center w-9 h-9 text-sm font-medium cursor-pointer rounded border transition-colors duration-150 mx-0.5 mb-1"
          :class="{
    'bg-green-100 border-green-400 text-green-800': answers[pq.questionId] !== undefined && answers[pq.questionId] !== null,
    'bg-gray-100 border-gray-300 text-gray-600': !(answers[pq.questionId] !== undefined && answers[pq.questionId] !== null)
  }"
      >
        {{ idx + 1 }}
      </div>
    </div>

    <!-- 提交按钮 -->
    <button
        class="mt-auto w-full py-2 px-3 rounded font-medium transition-colors duration-200"
        :class="{
            'bg-blue-600 hover:bg-blue-700 text-white cursor-pointer': allAnswered,
            'bg-gray-300 text-gray-500 cursor-not-allowed': !allAnswered
          }"
        @click="submitExam"
        :disabled="!allAnswered"
    >
      提交试卷
    </button>
  </div>
</div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onBeforeMount, onMounted, onUnmounted, ref, watch} from "vue"
import { useRoute } from "vue-router"
import { ExamPreparation, ExamCameraMonitor } from "../../components"
import { useRequest } from "vue-hooks-plus";
import {
  addExamAnswerAPI,
  getExam_PapersAPI,
  getExamDetailAPI,
  getExamSettingsAPI,
  getQuestionsByIdAPI,
  getQuestionTypeAPI,
  handleViolationAPI, judgeEligibleAPI, uploadInvigilationVideoAPI,
} from "../../apis";
import { ElNotification } from "element-plus";
import routers from "../../routers";

const route = useRoute()
const examId = ref<number>(Number(route.params.examId))

console.log('examId:', examId.value, '类型:', typeof examId.value);

const examToken = ref<string>('')   // 考试令牌

const questionTypes = ref([])

const exam = ref(); // 考试基本信息
const examSettings = ref(); // 考试设置信息
const examPaper = ref();  // 考试试卷信息
const questions = ref([]);  // 试卷题目

const answers = ref<Record<number, any>>({}) // key: questionId, value: 任意结构的答案

const attentionScore = ref<number>(0)

watch(()=>answers.value, (newValue) => {
  console.log('answers:', newValue);
}, {deep: true})

const hasQuestions = computed(() => {
  return examPaper.value.questions.some(item => item.question != null)
})

onBeforeMount(()=> {

  examToken.value = localStorage.getItem('examToken') // 获取已存储的考试令牌

  answers.value = JSON.parse(localStorage.getItem('localAnswers')) || {} // 加载时，先同步本地存储的答案，不需要全部重选

  judgeEligible()

  // examToken.value = "djasldjlakd";
  //
  // getQuestionTypes()
  // getExamDetail()
  // getExamPaper()
  // isFullScreen.value = true
  // isExamStarted.value = true
})

onUnmounted(()=>{

  localStorage.removeItem("examToken")
  localStorage.removeItem('localAnswers')
  isExamStarted.value = false
  if (countdownTimer) onclearTimeout(countdownTimer);
})

const judgeEligible = () => {
  useRequest(()=>judgeEligibleAPI(examId.value),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        getExamDetail() // 获取考试信息
      }else{
        routers.push('/exam').then(()=>{
          ElNotification.error({
            title: '提示',
            message: res['msg'],
            duration: 3000
          })
        })
      }
    }
  })
}

const getExamDetail = () => {
  useRequest(()=>getExamDetailAPI(examId.value), {  // 获取考试信息
    onSuccess: (res) => {
      if(res['code'] === 200){
        exam.value = res['data']
        console.log('exam:', exam.value);
      }
    }
  })
}

const remainingSeconds = ref(0);
let countdownTimer: number | null = null;

const startCountdown = (endTime: number) => {
  if(!isExamStarted.value) return

  const update = () => {
    if(!isExamStarted.value) return

    const now = Date.now();
    const left = Math.max(0, endTime - now);
    remainingSeconds.value = Math.ceil(left / 1000);

    console.log('remainingSeconds:', remainingSeconds.value);

    if (left <= 0) {
      addExamAnswer() // 超时自动提交
    } else {
      countdownTimer = window.setTimeout(update, 1000);
    }
  };

  update();
}

const getQuestionTypes = () => {
  useRequest(()=>getQuestionTypeAPI(),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        questionTypes.value = res['data']
      }
    }
  })
}

const getExamPaper = () => {
  useRequest(()=>getExam_PapersAPI(examId.value),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        examPaper.value = res['data']

        for(let i=0; i<examPaper.value.questions.length; i++){
          getQuestionDetail(examPaper.value.questions[i].questionId)  // 获取题目详情
        }

        console.log('examPaper:', examPaper.value);
      }else{
        ElNotification({title: 'Warning', message: res['msg'], type: 'warning',})
        if(res['msg']=="examToken无效"){
          localStorage.removeItem('examToken')
          examToken.value = ''
        }
      }
    }
  })
}

const getQuestionDetail = (questionId: number) => {
  useRequest(()=>getQuestionsByIdAPI({questionId: questionId}),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        examPaper.value.questions.find(item => item.questionId === questionId).question = res['data']
      }
    }
  })
}

// 实现一些考试设置
const getExamSettings = () => {
  useRequest(()=>getExamSettingsAPI(examId.value),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        examSettings.value = res['data']
        console.log('examSettings:', examSettings.value);

        if(examSettings.value['autoSubmit'] == 1){  // 自动提交开始计时
          startCountdown(exam.value.endTime)
        }
        if(examSettings.value['preventScreenSwitch'] == 1){  // 防止切屏
          console.log('防止切屏')
          preventScreenSwitch()
        }
      }
    }
  })
}

const isExamStarted = ref(false)
const isFullScreen = ref(false)

const preventScreenSwitch = () => {
  document.addEventListener('visibilitychange', ()=>{
    if (isExamStarted.value && document.hidden) {
      console.log('窗口被隐藏')
      handleViolation()
    }
  })
  window.addEventListener('blur', () => {
    if (isExamStarted.value && !document.hidden) {
      // 可能是 Alt+Tab、点击桌面等
      console.log('窗口失焦')
      // 同样可触发 leaveCount 增加（谨慎使用，可能误报）
      handleViolation()
    }
  })
  document.addEventListener('fullscreenchange', () => { // 监听退出全屏
    if (isExamStarted.value && !document.fullscreenElement) {
      // 用户退出了全屏 → 可能是作弊信号
      console.log('退出全屏')
      isFullScreen.value = false
      handleViolation()
    }
  })
}

const handleViolation = () => {
  useRequest(()=>handleViolationAPI(),{
    onSuccess: (res) => {
      if(res['code'] === 200){
        let cnt = Number(res['data'])
        console.log('违规次数:', cnt);
        if(cnt >= 3){
          exitExam()  // 清除数据，更改状态
          routers.push('/exam').then(()=>{
            window.location.reload();
            nextTick(()=>{
              ElNotification.error({
                title: '提示',
                message: '违规次数过多，请勿作弊！',
                duration: 3000
              })
            })
          })
        }else{
          ElNotification({title: 'Warning', message: '请勿作弊！', type: 'warning',})
        }
      }
    }
  })
}

const requestFullscreen = () => {
  const elem = document.documentElement; // 整个文档
  if (elem.requestFullscreen) {
    elem.requestFullscreen();
  } else if ((elem as any).webkitRequestFullscreen) { // Safari
    (elem as any).webkitRequestFullscreen();
  } else if ((elem as any).mozRequestFullScreen) { // Firefox
    (elem as any).mozRequestFullScreen();
  } else if ((elem as any).msRequestFullscreen) { // IE11
    (elem as any).msRequestFullscreen();
  }
}

// 分组逻辑
const groupedQuestions = computed(() => {

  if (!examPaper.value || !examPaper.value.questions) {
    return []
  }

  // 先按sortOrder进行排序
  const loadedItems = examPaper.value.questions
      .filter(item => item.question != null)
      .sort((a, b) => a.sortOrder - b.sortOrder)

  // 分组
  const groups: Record<number, { typeId: number; items: typeof loadedItems }> = {}
  for (const item of loadedItems) {
    const typeId = item.question.typeId
    if (!groups[typeId]) {
      groups[typeId] = { typeId, items: [] }
    }
    groups[typeId].items.push(item)
  }

  // 对每个组内的题目进行乱序（洗牌算法）
  if(examSettings.value != null && examSettings.value.questionShuffle == "1"){
    Object.values(groups).forEach(group => {
      group.items = shuffleArray(group.items)
    })
  }

  // console.log(Object.values(groups))
  return Object.values(groups)
})

// 随机打乱数组，不改变原数组
function shuffleArray<T>(array: T[]): T[] {
  const result = [...array] // 复制避免修改原数组
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

const getQuestionTypeName = (typeId: number) => {
  const type = questionTypes.value.find(t => t.typeId === typeId)
  return type ? type.typeName : '未知题型'
}

const componentCache = new Map<number, any>()

const getQuestionComponent = (typeId: number) => {
  if (componentCache.has(typeId)) {
    return componentCache.get(typeId)
  }

  const componentMap: Record<number, string> = {
    1: 'SingleChoiceRender',
    2: 'MultipleChoiceRender',
    3: 'TrueFalseRender',
    4: 'FillBlankRender',
    5: 'NounAnalysisRender',
    6: 'EssayQuestionRender',
    7: 'CalculationQuestionRender',
    8: 'AccountingEntryRender',
    9: 'MatchingQuestionRender',
    10: 'SortingQuestionRender',
    11: 'ClozeQuestionRender',
    12: 'ReadingComprehensionRender',
    17: 'PollQuestionRender',
  }

  const componentName = componentMap[typeId] || 'DefaultQuestionView'
  const asyncComp = defineAsyncComponent(() =>
      import(`../../components/question/render/${componentName}.vue`)
  )

  componentCache.set(typeId, asyncComp)
  return asyncComp
}

const verifyFaceSuccess = () => {
  examToken.value = localStorage.getItem('examToken')
  isExamStarted.value = true
  getQuestionTypes()  // 获取题目类型列表
  getExamPaper()  // 获取考试试卷
  getExamSettings() // 获取考试设置信息
}

// 所有题目是否都已作答
const allAnswered = computed(() => {
  return groupedQuestions.value.every(group =>
      group.items.every(pq => {
        return answers.value[pq.questionId] !== undefined && answers.value[pq.questionId] !== null
      })
  )

  return true;
})

// 滚动到具体题目
const scrollToQuestion = (questionId: number) => {
  window.document.getElementById(String(questionId))?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

// 提交试卷
const submitExam = () => {
  if (!allAnswered.value) {
    alert('请完成所有题目后再提交！')
    return
  }
  if(confirm("确定提交试卷？（提交后不可更改）")){
    addExamAnswer()
  }
}

const addExamAnswer = () => {
  useRequest(()=>addExamAnswerAPI({answers: answers.value, examId: examId.value}),{
    onBefore(){
      localStorage.setItem('localAnswers', JSON.stringify(answers.value)) // 保存本地答案
      isExamStarted.value = false
    },

    onSuccess(res){
      if(res['code']==200){
        answers.value = {}
        exitExam()
        routers.push("/exam").then(()=>{
          window.location.reload()
          nextTick(()=>{
            ElNotification({title: 'Success', message: '提交成功！', type: 'success',})
          })
        })
      }else{
        isExamStarted.value = true
        ElNotification({title: 'Warning', message: res['msg'], type: 'warning',})
      }
    },
    onError(err){
      isExamStarted.value = true
      ElNotification({title: 'Error', message: err.message, type: 'error',})
    },
  })
}

const uploadInvigilationVideo = (base64: string) => {
  useRequest(()=>uploadInvigilationVideoAPI({video: base64}),{
    onSuccess(res) {
      if (res['code'] == 200) {

        attentionScore.value = res['data']['attentionScore']

        if(res['data']['finish']){  // 结束考试（违规次数达到3次）
          exitExam()
          routers.push('/exam').then(()=>{
            window.location.reload()
            nextTick(()=>{
              alert("违规次数过多，请勿作弊！")
            })
          })
        }else if(res['data']['violation']){ // 监考违规（连续三次监考失败）
          ElNotification({title: 'Warning', message: "监考到违规操作", type: 'warning',})
        }
      }
    }
  })
}

const exitExam = () => {
  localStorage.removeItem('localAnswers') // 提交成功，删除本地答案
  localStorage.removeItem('examToken')  // 删除考试令牌
  isExamStarted.value = false
  examToken.value = ''
}
</script>

<style scoped>
</style>