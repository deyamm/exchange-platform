import { useRouter, type RouteLocationRaw } from 'vue-router'
import type { WatchTarget } from '/@/types/watchtarget/entity'

/**
 * 标的详情页面路由配置
 */
interface TargetNavigationOptions {

    // 指数详情页面路由名称
    indexRouteName?: string

    // 普通标的详情页面路由名称
    targetRouteName?: string

    // 自定义路由解析规则
    // 如果不同页面需要不同的跳转方式，可以通过该方法覆盖默认规则
    resolveRoute?: (
        target: WatchTarget,
        fullSymbol: string
    ) => RouteLocationRaw | null
}

/**
 * 标的搜索结果导航 Composable
 *
 * 负责：
 * 1. 接收 WatchTarget
 * 2. 解析完整标的代码
 * 3. 根据标的类型确定目标路由
 * 4. 执行页面跳转
 */
export function useTargetNavigation(
    options: TargetNavigationOptions = {}
) {

    const router = useRouter()

    /**
     * 获取标的完整代码
     *
     * 例如：
     * targetCode = 000001
     * marketCode = SZ
     *
     * 返回：
     * 000001.SZ
     *
     * 如果 targetCode 已经包含市场后缀，则直接返回。
     */
    function getTargetFullSymbol(target: WatchTarget): string {

        const code = target.targetCode?.trim()

        if (!code) {
            return ''
        }

        if (code.includes('.')) {
            return code
        }

        const marketCode = target.marketCode?.trim()

        return marketCode
            ? `${code}.${marketCode}`
            : code
    }

    /**
     * 根据标的类型解析目标路由
     */
    function resolveTargetRoute(
        target: WatchTarget
    ): RouteLocationRaw | null {

        const fullSymbol = getTargetFullSymbol(target)

        if (!fullSymbol) {
            return null
        }

        // 如果外部传入了自定义路由解析方法，优先使用
        if (options.resolveRoute) {
            return options.resolveRoute(target, fullSymbol)
        }

        // 指数类型
        if (target.targetType?.toUpperCase() === 'INDEX') {

            return {
                name: options.indexRouteName || 'indexDetail',
                params: {
                    fullSymbol
                }
            }
        }

        // 其他普通标的类型
        return {
            name: options.targetRouteName || 'targetDetail',
            params: {
                targetCode: fullSymbol
            }
        }
    }

    /**
     * 跳转到标的详情页面
     */
    async function navigateToTarget(
        target: WatchTarget
    ): Promise<boolean> {

        const route = resolveTargetRoute(target)

        if (!route) {
            console.warn('无法解析标的详情路由', target)
            return false
        }

        try {

            const failure = await router.push(route)

            return !failure

        } catch (error) {

            console.error('标的详情页面跳转失败', error)

            return false
        }
    }

    return {
        getTargetFullSymbol,
        resolveTargetRoute,
        navigateToTarget
    }
}