import { RouteRecordRaw } from 'vue-router';

/**
 * 建议：路由 path 路径与文件夹名称相同，找文件可浏览器地址找，方便定位文件位置
 *
 * 路由meta对象参数说明
 * meta: {
 *      title:          菜单栏及 tagsView 栏、菜单搜索名称（国际化）
 *      isLink：        是否超链接菜单，开启外链条件，`1、isLink: 链接地址不为空 2、isIframe:false`
 *      isHide：        是否隐藏此路由
 *      isKeepAlive：   是否缓存组件状态
 *      isAffix：       是否固定在 tagsView 栏上
 *      isIframe：      是否内嵌窗口，开启条件，`1、isIframe:true 2、isLink：链接地址不为空`
 *      roles：         当前路由权限标识，取角色管理。控制路由显示、隐藏。超级管理员：admin 普通角色：common
 *      icon：          菜单、tagsView 图标，阿里：加 `iconfont xxx`，fontawesome：加 `fa xxx`
 * }
 */

// 扩展 RouteMeta 接口
declare module 'vue-router' {
	interface RouteMeta {
		title?: string;
		isLink?: string;
		isHide?: boolean;
		isKeepAlive?: boolean;
		isAffix?: boolean;
		isIframe?: boolean;
		roles?: string[];
		icon?: string;
	}
}

/**
 * 定义动态路由
 * 前端添加路由，请在顶级节点的 `children 数组` 里添加
 * @description 未开启 isRequestRoutes 为 true 时使用（前端控制路由），开启时第一个顶级 children 的路由将被替换成接口请求回来的路由数据
 * @description 各字段请查看 `/@/views/system/menu/component/addMenu.vue 下的 ruleForm`
 * @returns 返回路由菜单数据
 */
export const dynamicRoutes: Array<RouteRecordRaw> = [
	{
		path: '/',
		name: '/',
		component: () => import('/@/layout/index.vue'),
		redirect: '/home',
		meta: {
			isKeepAlive: true,
		},
		children: [
			{
				path: '/app/ranking', name: 'ranking', component: () => import('../views/appconfig/ranking/index.vue'),
				meta: { title: '指标排行', isLink: '', isHide: false, isKeepAlive: true, isAffix: false, isIframe: false, roles: ['admin', 'common'], icon: 'ele-DataAnalysis' },
			},
			{
				path: '/app/ranking-compare', name: 'rankingCompare', component: () => import('../views/appconfig/ranking/compare.vue'),
				meta: { title: '标的对比', isLink: '', isHide: true, isKeepAlive: false, isAffix: false, isIframe: false, roles: ['admin', 'common'], icon: 'ele-DataAnalysis' },
			},
			{
				path: '/home',
				name: 'home',
				component: () => import('/@/views/home/index.vue'),
				meta: {
					title: 'message.router.home',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: true,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-shouye',
				},
			},
			{
				path: '/datasource',
				name: 'dataSource',
				component: () => import('/@/views/datasource/DataSourcePage.vue'),
				meta: {
					title: 'message.router.dataSource',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-shujutopic',
				},
			},
			{
				path: '/collectionConfig',
				name: 'collectionConfig',
				component: () => import('../views/collectionconfig/index.vue'),
				redirect: '/collectionConfig/datatopic',
				meta: {
					title: 'message.router.collectionConfig',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-yingyongpeizhi',
				},
				children: [
					{
						path: '/collectionMonitor/datasets', name: 'collectionDatasets', component: () => import('../views/collectionMonitor/dataset/DatasetPage.vue'),
						meta: { title: '数据集管理', isLink: '', isHide: false, isKeepAlive: true, isAffix: false, isIframe: false, roles: ['admin', 'common'], icon: 'ele-Files' },
					},
					{
						path: '/collectionMonitor/datasets/:datasetCode', name: 'collectionDatasetDetail', component: () => import('../views/collectionMonitor/dataset/DatasetDetailPage.vue'),
						meta: { title: '数据集详情', isLink: '', isHide: true, isKeepAlive: false, isAffix: false, isIframe: false, roles: ['admin', 'common'], icon: 'ele-Files' },
					},
					{
						path: '/collectionConfig/datatopic',
						name: 'datatopic',
						component: () => import('../views/collectionconfig/datatopic/DataTopicPage.vue'),
						meta: {
							title: 'message.router.datatopic',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-shujutopic',
						},
					},
					{
						path: '/collectionConfig/datatype',
						name: 'datatype',
						component: () => import('../views/collectionconfig/datatype/DataTypePage.vue'),
						meta: {
							title: 'message.router.datatype',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-shujutopic',
						},
					},
					{
						path: '/collectionConfig/collectionTask',
						name: 'collectionTask',
						component: () => import('../views/collectionconfig/collectiontask/TaskPage.vue'),
						meta: {
							title: 'message.router.collectionTask',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-采集任务',
						},
					},
					{
						path: '/collectionConfig/collectionTask/detail/:taskCode',
						name: 'collectionTaskDetail',
						component: () => import('../views/collectionconfig/collectiontask/TaskDetailPage.vue'),
						meta: {
							title: 'message.router.collectionTaskDetail',
							isLink: '',
							isHide: true, // 详情页不在菜单显示
							isKeepAlive: false, // 详情页不需要缓存
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-采集任务',
						},
					},
					{
						path: '/collectionConfig/collectionTask/config/:taskCode',
						name: 'collectionTaskConfig',
						component: () => import('../views/collectionconfig/collectiontask/TaskConfigPage.vue'),
						meta: {
							title: 'message.router.collectionTaskConfig',
							isLink: '',
							isHide: true, // 详情页不在菜单显示
							isKeepAlive: false, // 详情页不需要缓存
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-采集任务',
						},
					},
				]
			},
			{
				path: '/collectionExecute',
				name: 'collectionExecute',
				component: () => import('../views/collectionexecute/index.vue'),
				redirect: '/collectionExecute/taskCreate',
				meta: {
					title: 'message.router.collectionExecute',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-yingyongpeizhi',
				},
				children: [
					{
						path: '/collectionExecute/taskCreate',
						name: 'collectionTaskCreate',
						component: () => import('../views/collectionexecute/execution/TaskCreatePage.vue'),
						meta: {
							title: 'message.router.collectionTaskCreate',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-采集任务',
						},
					},
					{
						path: '/collectionExecute/taskScheduler',
						name: 'collectionTaskScheduler',
						component: () => import('../views/collectionexecute/execution/ExecutionSchedulerPage.vue'),
						meta: {
							title: 'message.router.collectionTaskScheduler',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-AlarmClock',
						},
					},
				]
			},	
			{
				path: '/collectionMonitor',
				name: 'collectionMonitor',
				component: () => import('../views/collectionMonitor/index.vue'),
				redirect: '/collectionMonitor/taskMonitor',
				meta: {
					title: 'message.router.collectionMonitor',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-采集任务',
				},
				children: [
					{
						path: '/collectionMonitor/taskMonitor',
						name: 'collectionTaskMonitor',
						component: () => import('../views/collectionMonitor/monitor/MonitorPage.vue'),
						meta: {
							title: 'message.router.collectionTaskMonitor',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-采集任务',
						},
					},
				]
			},
			{
				path: '/appConfig',
				name: 'appConfig',
				component: () => import('/@/views/appconfig/index.vue'),
				redirect: '/appConfig/appScene',
				meta: {
					title: 'message.router.appConfig',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-yingyongpeizhi',
				},
				children: [
					{
						path: '/appConfig/filtering',
						name: 'pagesFiltering',
						component: () => import('../views/appconfig/filtering/index.vue'),
						meta: {
							title: 'message.router.pagesFiltering',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Sell',
						},
					},
					{
						path: '/appConfig/alertRule',
						name: 'appAlertRule',
						component: () => import('../views/appconfig/alertrule/AlertRulePage.vue'),
						meta: {
							title: 'message.router.appAlertRule',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Sell',
						},
					},
					{
						path: '/appConfig/ranking',
						name: 'appRanking',
						component: () => import('../views/appconfig/ranking/index.vue'),
						meta: {
							title: 'message.router.appRanking',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Sell',
						},
					},
					{
						path: '/appConfig/watchTarget',
						name: 'watchTarget',
						component: () => import('/@/views/appconfig/watchtarget/WatchTargetPage.vue'),
						meta: {
							title: 'message.router.watchTarget',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongpeizhi',
						},
					},
					{
						path: '/appConfig/appMetric',
						name: 'appMetric',
						component: () => import('/@/views/appconfig/appmetric/AppMetricPage.vue'),
						meta: {
							title: 'message.router.appMetric',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongpeizhi',
						},
					},
					{
						path: '/appConfig/researchTag',
						name: 'researchTag',
						component: () => import('/@/views/appconfig/researchtag/ResearchTagPage.vue'),
						meta: {
							title: 'message.router.researchTag',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongpeizhi',
						},
					}
				]
			},
			{
				path: '/appDisplay',
				name: 'appDisplay',
				component: () => import('/@/views/appdisplay/index.vue'),
				redirect: '/appDisplay/marketOverview',
				meta: {
					title: 'message.router.appDisplay',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-yingyongfenxiyudisplay',
				},
				children: [
					{
						path: '/appDisplay/marketOverview',
						name: 'marketOverview',
						component: () => import('/@/views/appdisplay/marketoverview/MarketOverviewPage.vue'),
						meta: {
							title: 'message.router.marketOverview',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					},
					{
						path: '/appDisplay/marketOverview/indexBasicExplorer',
						name: 'indexBasicExplorer',
						component: () => import('/@/views/appdisplay/marketoverview/IndexBasicExplorer.vue'),
						meta: {
							title: 'message.router.indexBasicExplorer',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					},
					// 用于调试
					{
						path: '/appDisplay/targetDetail',
						name: 'targetDetail',
						component: () => import('/@/views/appdisplay/targetdetail/TargetDetailPage.vue'),
						meta: {
							title: 'message.router.targetDetail',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					},
					{
						path: '/appDisplay/targetDetail/:targetCode?',
						name: 'targetDetailByCode',
						component: () => import('/@/views/appdisplay/targetdetail/TargetDetailPage.vue'),
						meta: {
							title: 'message.router.targetDetail',
							isLink: '',
							isHide: true,
							isKeepAlive: false,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					},
					{
						path: '/appDisplay/indexDetail',
						name: 'indexDetail',
						component: () => import('/@/views/appdisplay/indexdetail/IndexDetailPage.vue'),
						meta: {
							title: 'message.router.indexDetail',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					},
					{
						path: '/appDisplay/indexDetail/:fullSymbol?',
						name: 'indexDetailByCode',
						component: () => import('/@/views/appdisplay/indexdetail/IndexDetailPage.vue'),
						meta: {
							title: 'message.router.indexDetail',
							isLink: '',
							isHide: true,
							isKeepAlive: false,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-yingyongfenxiyudisplay',
						},
					}
				]
			}
		],
	},
];

/**
 * 定义404、401界面
 * @link 参考：https://next.router.vuejs.org/zh/guide/essentials/history-mode.html#netlify
 */
export const notFoundAndNoPower = [
	{
		path: '/:path(.*)*',
		name: 'notFound',
		component: () => import('/@/views/error/404.vue'),
		meta: {
			title: 'message.staticRoutes.notFound',
			isHide: true,
		},
	},
	{
		path: '/401',
		name: 'noPower',
		component: () => import('/@/views/error/401.vue'),
		meta: {
			title: 'message.staticRoutes.noPower',
			isHide: true,
		},
	},
];

/**
 * 定义静态路由（默认路由）
 * 此路由不要动，前端添加路由的话，请在 `dynamicRoutes 数组` 中添加
 * @description 前端控制直接改 dynamicRoutes 中的路由，后端控制不需要修改，请求接口路由数据时，会覆盖 dynamicRoutes 第一个顶级 children 的内容（全屏，不包含 layout 中的路由出口）
 * @returns 返回路由菜单数据
 */
export const staticRoutes: Array<RouteRecordRaw> = [
	{
		path: '/login',
		name: 'login',
		component: () => import('/@/views/login/index.vue'),
		meta: {
			title: '登录',
		},
	},
	/**
	 * 提示：写在这里的为全屏界面，不建议写在这里
	 * 请写在 `dynamicRoutes` 路由数组中
	 */
	{
		path: '/visualizingDemo1',
		name: 'visualizingDemo1',
		component: () => import('/@/views/visualizing/demo1.vue'),
		meta: {
			title: 'message.router.visualizingLinkDemo1',
		},
	},
	{
		path: '/visualizingDemo2',
		name: 'visualizingDemo2',
		component: () => import('/@/views/visualizing/demo2.vue'),
		meta: {
			title: 'message.router.visualizingLinkDemo2',
		},
	},
];
