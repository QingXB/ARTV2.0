<template>
  <div class="similarity-graph-container">
    <div class="graph-header">
      <div class="header-left">
        <h3>📊 语义相似度图谱</h3>
        <span class="hint">节点代表文献，连线粗细表示相似度</span>
      </div>
      <div class="header-right">
        <div class="threshold-control">
          <label>相似度阈值:</label>
          <input 
            type="range" 
            v-model="threshold" 
            min="0" 
            max="1" 
            step="0.05" 
            @change="refreshGraph"
          />
          <span>{{ threshold }}</span>
        </div>
        <button class="btn-refresh" @click="refreshGraph">🔄 刷新图谱</button>
      </div>
    </div>

    <div class="graph-toolbar">
      <span class="toolbar-item" :class="{ active: !isSelecting }" @click="toggleSelectMode(false)">
        👆 选择
      </span>
      <span class="toolbar-item" @click="resetSelection">
        🗑️ 清除选择
      </span>
    </div>

    <div ref="chartRef" class="chart-container"></div>

    <div v-if="selectedNodes.length > 0" class="selected-panel">
      <h4>已选中文献 ({{ selectedNodes.length }}篇)</h4>
      <div class="selected-list">
        <div v-for="node in selectedNodes" :key="node.id" class="selected-item">
          <span class="node-title">{{ node.title }}</span>
          <button type="button" @click="removeFromSelection(node.id)">×</button>
        </div>
      </div>
      <button type="button" class="send-btn" :class="{ disabled: selectedNodes.length < 2 }" :disabled="selectedNodes.length < 2" @click="sendToReview">
        📝 发送 {{ selectedNodes.length }} 篇到综述
      </button>
    </div>
  </div>
</template>

<script setup>import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import request from '@/utils/request';
const emit = defineEmits(['sendToReview']);
const chartRef = ref(null);
let chartInstance = null;
const threshold = ref(0.5);
const isSelecting = ref(false);
const selectedNodes = ref([]);
const graphData = ref({ nodes: [], edges: [] });
const loadGraph = async () => {
 try {
 const token = localStorage.getItem('token') || sessionStorage.getItem('token');
 const res = await request.get(`/api/graph/similarity?threshold=${threshold.value}`, {
 headers: { 'Authorization': `Bearer ${token}` }
 });
 graphData.value = res;
 renderChart();
 }
 catch (error) {
 console.error('加载图谱失败:', error);
 }
};
const refreshGraph = () => {
 loadGraph();
};
const toggleSelectMode = (mode) => {
 isSelecting.value = mode;
 if (chartInstance) {
 if (mode) {
 chartInstance.dispatchAction({
 type: 'takeGlobalCursor',
 key: 'dataZoomSelect',
 dataZoomIndex: [0, 1]
 });
 }
 else {
 chartInstance.dispatchAction({
 type: 'takeGlobalCursor',
 key: 'default'
 });
 }
 }
};
const resetSelection = () => {
 const nodeNames = selectedNodes.value.map(n => `Paper_${n.id}`);
 selectedNodes.value = [];
 if (chartInstance) {
 chartInstance.dispatchAction({
 type: 'unselect',
 seriesIndex: 0,
 name: nodeNames
 });
 }
};
const removeFromSelection = (nodeId) => {
 console.log('removeFromSelection called with nodeId:', nodeId);
 console.log('Before filter - selectedNodes:', selectedNodes.value);
 selectedNodes.value = selectedNodes.value.filter(n => n.id !== nodeId);
 console.log('After filter - selectedNodes:', selectedNodes.value);
 if (chartInstance) {
 chartInstance.dispatchAction({
 type: 'unselect',
 seriesIndex: 0,
 name: [`Paper_${nodeId}`]
 });
 }
};
const zoomIn = () => {
 if (chartInstance) {
 const option = chartInstance.getOption();
 const currentZoom = option.series[0].roamScale || 1;
 chartInstance.dispatchAction({
 type: 'dataZoom',
 start: (100 - 100 / (currentZoom * 1.3)) / 2,
 end: 100 - (100 - 100 / (currentZoom * 1.3)) / 2
 });
 }
};
const zoomOut = () => {
 if (chartInstance) {
 const option = chartInstance.getOption();
 const currentZoom = option.series[0].roamScale || 1;
 chartInstance.dispatchAction({
 type: 'dataZoom',
 start: (100 - 100 / (currentZoom * 0.7)) / 2,
 end: 100 - (100 - 100 / (currentZoom * 0.7)) / 2
 });
 }
};
const resetView = () => {
 if (chartInstance) {
 chartInstance.dispatchAction({
 type: 'dataZoom',
 start: 0,
 end: 100
 });
 }
};
const sendToReview = () => {
 const paperIds = selectedNodes.value.map(n => n.id);
 emit('sendToReview', paperIds);
};
const renderChart = () => {
 if (!chartInstance)
 return;
 const nodes = graphData.value.nodes.map(node => ({
 id: parseInt(node.id),
 name: `Paper_${node.id}`,
 symbolSize: 30 + (node.summary?.length || 0) / 20,
 category: node.category || 0,
 itemStyle: {
 color: getNodeColor(node)
 },
 label: {
 show: true,
 fontSize: 12,
 formatter: (params) => {
 const title = node.title || '';
 return title.length > 10 ? title.substring(0, 10) + '...' : title;
 }
 },
 data: node
 }));
 const edges = graphData.value.edges.map(edge => ({
 source: edge.source,
 target: edge.target,
 value: edge.weight,
 lineStyle: {
 width: edge.weight * 8,
 color: getEdgeColor(edge.weight),
 opacity: 0.6 + edge.weight * 0.4
 }
 }));
 const option = {
 backgroundColor: 'transparent',
 tooltip: {
 trigger: 'item',
 backgroundColor: 'linear-gradient(135deg, #ffffff 0%, #f8fafc 100%)',
 borderColor: 'rgba(99, 102, 241, 0.2)',
 borderWidth: 1,
 padding: [12, 16],
 width: 300,
 align: 'left',
 textStyle: {
 color: '#1e293b',
 fontSize: 13,
 lineHeight: 1.6,
 whiteSpace: 'normal',
 textAlign: 'left'
 },
 extraCssText: 'border-radius: 12px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08); backdrop-filter: blur(10px); max-width: 320px; text-align: left;',
 formatter: (params) => {
 if (params.dataType === 'node') {
 const node = params.data.data;
 const title = node.title || '未知文献';
 return `
 <div style="width: 100%; max-width: 300px;">
 <div style="margin-bottom: 10px;">
 <div style="display: flex; align-items: flex-start; gap: 8px;">
 <div style="width: 8px; height: 8px; border-radius: 50%; background: ${node.parseStatus === 2 ? '#6366f1' : node.parseStatus === 1 ? '#f59e0b' : '#94a3b8'}; flex-shrink: 0; margin-top: 5px;"></div>
 </div>
 <h4 style="margin: 0; color: #6366f1; font-size: 14px; font-weight: 600; line-height: 1.5; word-break: break-all; white-space: pre-wrap;">📄 ${title}</h4>
 </div>
 ${node.author ? `<p style="margin: 5px 0; color: #64748b; font-size: 12px; word-break: break-all;">👤 ${node.author}</p>` : ''}
 ${node.publishYear ? `<p style="margin: 5px 0; color: #64748b; font-size: 12px;">📅 ${node.publishYear}年</p>` : ''}
 ${node.summary ? `
 <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(99, 102, 241, 0.15);">
 <div style="font-size: 12px; color: #6366f1; font-weight: 500; margin-bottom: 5px;">核心内容</div>
 <p style="margin: 0; font-size: 12px; color: #475569; line-height: 1.6; word-break: break-all; white-space: pre-wrap;">${node.summary}</p>
 </div>
 ` : ''}
 </div>
 `;
 }
 else if (params.dataType === 'edge') {
 const weight = params.value;
 const color = weight >= 0.8 ? '#10b981' : weight >= 0.6 ? '#6366f1' : '#f59e0b';
 return `
 <div style="padding: 8px 12px; min-width: 140px;">
 <div style="display: flex; align-items: center; gap: 8px;">
 <span style="font-size: 14px;">🔗</span>
 <span style="color: #64748b; font-size: 13px;">相似度:</span>
 <strong style="color: ${color}; font-size: 14px; font-weight: 600;">${(weight * 100).toFixed(1)}%</strong>
 </div>
 <div style="margin-top: 6px; height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden;">
 <div style="height: 100%; width: ${weight * 100}%; background: linear-gradient(90deg, ${color} 0%, ${color}dd 100%); border-radius: 3px;"></div>
 </div>
 </div>
 `;
 }
 return '';
 }
 },
 legend: {
 data: ['已解析', '待解析'],
 orient: 'vertical',
 right: 20,
 top: 20,
 textStyle: {
 color: '#64748b'
 }
 },
 series: [{
 type: 'graph',
 layout: 'force',
 animation: true,
 animationDuration: 1500,
 animationEasingUpdate: 'quinticInOut',
 roam: true,
 draggable: true,
 force: {
 repulsion: 300,
 gravity: 0.1,
 edgeLength: [50, 200],
 friction: 0.6
 },
 data: nodes,
 links: edges,
 categories: [{
 name: '已解析',
 itemStyle: { color: '#6366f1' }
 }, {
 name: '待解析',
 itemStyle: { color: '#94a3b8' }
 }],
 emphasis: {
 focus: 'adjacency',
 lineStyle: {
 width: 10
 },
 itemStyle: {
 shadowBlur: 20,
 shadowColor: 'rgba(99, 102, 241, 0.5)'
 }
 },
 select: {
 itemStyle: {
 color: '#f59e0b',
 borderColor: '#d97706',
 borderWidth: 3
 }
 },
 selectedMode: 'multiple',
 label: {
 position: 'bottom',
 distance: 5
 },
 lineStyle: {
 curveness: 0.2
 }
 }]
 };
 chartInstance.setOption(option, true);
};
const getNodeColor = (node) => {
 if (node.parseStatus === 2) {
 return '#6366f1';
 }
 else if (node.parseStatus === 1) {
 return '#f59e0b';
 }
 else if (node.parseStatus === 3) {
 return '#ef4444';
 }
 return '#94a3b8';
};
const getEdgeColor = (weight) => {
 if (weight >= 0.8) {
 return '#10b981';
 }
 else if (weight >= 0.6) {
 return '#6366f1';
 }
 else if (weight >= 0.5) {
 return '#f59e0b';
 }
 return '#94a3b8';
};
const initChart = () => {
 if (chartRef.value) {
 chartInstance = echarts.init(chartRef.value);
 chartInstance.on('click', (params) => {
 if (params.dataType === 'node') {
 const nodeId = parseInt(params.data.id);
 const isSelected = selectedNodes.value.some(n => n.id === nodeId);
 if (isSelected) {
 removeFromSelection(nodeId);
 }
 else {
 const nodeData = graphData.value.nodes.find(n => n.id === nodeId);
 if (nodeData) {
 selectedNodes.value.push(nodeData);
 }
 }
 }
 });
 chartInstance.on('brushEnd', (params) => {
 if (params.batch && params.batch[0]) {
 const selectedNames = params.batch[0].selected[0];
 const newSelected = [];
 selectedNames.forEach(name => {
 const match = name.match(/Paper_(\d+)/);
 if (match) {
 const nodeId = parseInt(match[1]);
 if (!selectedNodes.value.some(n => n.id === nodeId)) {
 const nodeData = graphData.value.nodes.find(n => n.id === nodeId);
 if (nodeData) {
 newSelected.push(nodeData);
 }
 }
 }
 });
 selectedNodes.value = [...selectedNodes.value, ...newSelected];
 }
 });
 window.addEventListener('resize', () => {
 chartInstance?.resize();
 });
 }
};
onMounted(() => {
 initChart();
 loadGraph();
});
onUnmounted(() => {
 chartInstance?.dispose();
});
watch(threshold, () => {
 refreshGraph();
});
</script>

<style scoped>
.similarity-graph-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.header-left .hint {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.threshold-control {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.threshold-control input[type="range"] {
  width: 120px;
}

.btn-refresh,
.btn-send-to-review {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh {
  background: #f1f5f9;
  color: #64748b;
}

.btn-refresh:hover {
  background: #e2e8f0;
}

.btn-send-to-review {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  box-shadow: 0 2px 6px -1px rgba(99, 102, 241, 0.35);
}

.btn-send-to-review:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px -2px rgba(99, 102, 241, 0.45);
}

.graph-toolbar {
  display: flex;
  gap: 4px;
  padding: 10px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.toolbar-item {
  padding: 6px 12px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.toolbar-item:hover {
  background: #e2e8f0;
}

.toolbar-item.active {
  background: #6366f1;
  color: white;
}

.chart-container {
  flex: 1;
  min-height: 400px;
}

.selected-panel {
  padding: 12px 20px;
  background: #fef3c7;
  border-top: 1px solid #fcd34d;
}

.selected-panel h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #92400e;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  color: #475569;
}

.selected-item button {
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  line-height: 1;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.selected-item button:hover {
  background: #ef4444;
  color: white;
}

.send-btn {
  margin-top: 12px;
  margin-left: auto;
  padding: 8px 20px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: block;
}

.send-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.send-btn:active {
  transform: translateY(0);
}

.send-btn.disabled {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
}

.send-btn.disabled:hover {
  transform: none;
  box-shadow: none;
}
</style>