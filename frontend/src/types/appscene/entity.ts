import { CommonStatus, AppSceneType } from "./constants"

// ---------------- A-01 应用场景 ----------------

export interface AppScene {
    sceneCode: string
    sceneName: string
    sceneType?: AppSceneType
    sortNo?: number
    status?: CommonStatus
    description?: string
    createdAt?: string
}

export function createDefaultAppScene(): AppScene {
    return {
        sceneCode: '',
        sceneName: '',
        sceneType: 'MARKET_OVERVIEW',
        sortNo: 0,
        status: 'DRAFT',
        description: '',
        createdAt: ''
    };
}