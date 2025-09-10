// src/config/heatmap.ts
export const CHUNK_SIZE = 9;

export const HEAT_TIERS = {
  HOT: { 
    threshold: 51, 
    color: '#FF4500', 
    label: '热区 (51-81 条消息)' 
  },
  WARM: { 
    threshold: 21, 
    color: '#FFD700', 
    label: '暖区 (21-50 条消息)' 
  },
  COLD: { 
    threshold: 0,  
    color: '#1E90FF', 
    label: '冷区 (0-20 条消息)' 
  }
};

export const MINIMAP_SIZE = 200;
export const MINIMAP_CHUNKS_VISIBLE = 15; // 小地图显示的区块范围

/**
 * 根据热力值获取对应的颜色
 */
export function getHeatColor(heatValue: number): string {
  if (heatValue >= HEAT_TIERS.HOT.threshold) {
    return HEAT_TIERS.HOT.color;
  } else if (heatValue >= HEAT_TIERS.WARM.threshold) {
    return HEAT_TIERS.WARM.color;
  } else if (heatValue > 0) {
    return HEAT_TIERS.COLD.color;
  }
  return 'transparent';
}

/**
 * 根据热力值获取对应的等级
 */
export function getHeatTier(heatValue: number): keyof typeof HEAT_TIERS {
  if (heatValue >= HEAT_TIERS.HOT.threshold) {
    return 'HOT';
  } else if (heatValue >= HEAT_TIERS.WARM.threshold) {
    return 'WARM';
  } else if (heatValue > 0) {
    return 'COLD';
  }
  return 'COLD';
}