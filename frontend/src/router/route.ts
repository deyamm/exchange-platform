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
						path: '/appConfig/appScene',
						name: 'appScene',
						component: () => import('/@/views/appconfig/appscene/AppScenePage.vue'),
						meta: {
							title: 'message.router.appScene',
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
						path: '/appConfig/viewConfig',
						name: 'viewConfig',
						component: () => import('/@/views/appconfig/viewconfig/ViewConfigPage.vue'),
						meta: {
							title: 'message.router.viewConfig',
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
						path: '/appConfig/alertRule',
						name: 'alertRule',
						component: () => import('/@/views/appconfig/alertrule/AlertRulePage.vue'),
						meta: {
							title: 'message.router.alertRule',
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
					}
				]
			},
			{
				path: '/system',
				name: 'system',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/system/menu',
				meta: {
					title: 'message.router.system',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin'],
					icon: 'iconfont icon-xitongshezhi',
				},
				children: [
					{
						path: '/system/menu',
						name: 'systemMenu',
						component: () => import('/@/views/system/menu/index.vue'),
						meta: {
							title: 'message.router.systemMenu',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'iconfont icon-caidan',
						},
					},
					{
						path: '/system/role',
						name: 'systemRole',
						component: () => import('/@/views/system/role/index.vue'),
						meta: {
							title: 'message.router.systemRole',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-ColdDrink',
						},
					},
					{
						path: '/system/user',
						name: 'systemUser',
						component: () => import('/@/views/system/user/index.vue'),
						meta: {
							title: 'message.router.systemUser',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'iconfont icon-icon-',
						},
					},
					{
						path: '/system/dept',
						name: 'systemDept',
						component: () => import('/@/views/system/dept/index.vue'),
						meta: {
							title: 'message.router.systemDept',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-OfficeBuilding',
						},
					},
					{
						path: '/system/dic',
						name: 'systemDic',
						component: () => import('/@/views/system/dic/index.vue'),
						meta: {
							title: 'message.router.systemDic',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-SetUp',
						},
					},
				],
			},
			{
				path: '/limits',
				name: 'limits',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/limits/frontEnd',
				meta: {
					title: 'message.router.limits',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-quanxian',
				},
				children: [
					{
						path: '/limits/frontEnd',
						name: 'limitsFrontEnd',
						component: () => import('/@/layout/routerView/parent.vue'),
						redirect: '/limits/frontEnd/page',
						meta: {
							title: 'message.router.limitsFrontEnd',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: '',
						},
						children: [
							{
								path: '/limits/frontEnd/page',
								name: 'limitsFrontEndPage',
								component: () => import('/@/views/limits/frontEnd/page/index.vue'),
								meta: {
									title: 'message.router.limitsFrontEndPage',
									isLink: '',
									isHide: false,
									isKeepAlive: true,
									isAffix: false,
									isIframe: false,
									roles: ['admin', 'common'],
									icon: '',
								},
							},
							{
								path: '/limits/frontEnd/btn',
								name: 'limitsFrontEndBtn',
								component: () => import('/@/views/limits/frontEnd/btn/index.vue'),
								meta: {
									title: 'message.router.limitsFrontEndBtn',
									isLink: '',
									isHide: false,
									isKeepAlive: true,
									isAffix: false,
									isIframe: false,
									roles: ['admin', 'common'],
									icon: '',
								},
							},
						],
					},
					{
						path: '/limits/backEnd',
						name: 'limitsBackEnd',
						component: () => import('/@/layout/routerView/parent.vue'),
						meta: {
							title: 'message.router.limitsBackEnd',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: '',
						},
						children: [
							{
								path: '/limits/backEnd/page',
								name: 'limitsBackEndEndPage',
								component: () => import('/@/views/limits/backEnd/page/index.vue'),
								meta: {
									title: 'message.router.limitsBackEndEndPage',
									isLink: '',
									isHide: false,
									isKeepAlive: true,
									isAffix: false,
									isIframe: false,
									roles: ['admin', 'common'],
									icon: '',
								},
							},
						],
					},
				],
			},
			{
				path: '/fun',
				name: 'funIndex',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/fun/tagsView',
				meta: {
					title: 'message.router.funIndex',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-crew_feature',
				},
				children: [
					{
						path: '/fun/tagsView',
						name: 'funTagsView',
						component: () => import('/@/views/fun/tagsView/index.vue'),
						meta: {
							title: 'message.router.funTagsView',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Pointer',
						},
					},
					{
						path: '/fun/countup',
						name: 'funCountup',
						component: () => import('/@/views/fun/countup/index.vue'),
						meta: {
							title: 'message.router.funCountup',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Odometer',
						},
					},
					{
						path: '/fun/wangEditor',
						name: 'funWangEditor',
						component: () => import('/@/views/fun/wangEditor/index.vue'),
						meta: {
							title: 'message.router.funWangEditor',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-fuwenbenkuang',
						},
					},
					{
						path: '/fun/cropper',
						name: 'funCropper',
						component: () => import('/@/views/fun/cropper/index.vue'),
						meta: {
							title: 'message.router.funCropper',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-caijian',
						},
					},
					{
						path: '/fun/qrcode',
						name: 'funQrcode',
						component: () => import('/@/views/fun/qrcode/index.vue'),
						meta: {
							title: 'message.router.funQrcode',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-ico',
						},
					},
					{
						path: '/fun/echartsMap',
						name: 'funEchartsMap',
						component: () => import('/@/views/fun/echartsMap/index.vue'),
						meta: {
							title: 'message.router.funEchartsMap',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-ditu',
						},
					},
					{
						path: '/fun/printJs',
						name: 'funPrintJs',
						component: () => import('/@/views/fun/printJs/index.vue'),
						meta: {
							title: 'message.router.funPrintJs',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Printer',
						},
					},
					{
						path: '/fun/clipboard',
						name: 'funClipboard',
						component: () => import('/@/views/fun/clipboard/index.vue'),
						meta: {
							title: 'message.router.funClipboard',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-DocumentCopy',
						},
					},
					{
						path: '/fun/gridLayout',
						name: 'funGridLayout',
						component: () => import('/@/views/fun/gridLayout/index.vue'),
						meta: {
							title: 'message.router.funGridLayout',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-tuodong',
						},
					},
					{
						path: '/fun/splitpanes',
						name: 'funSplitpanes',
						component: () => import('/@/views/fun/splitpanes/index.vue'),
						meta: {
							title: 'message.router.funSplitpanes',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon--chaifenlie',
						},
					},
				],
			},
			{
				path: '/pages',
				name: 'pagesIndex',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/pages/filtering',
				meta: {
					title: 'message.router.pagesIndex',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin', 'common'],
					icon: 'iconfont icon-fuzhiyemian',
				},
				children: [
					{
						path: '/pages/filtering',
						name: 'pagesFiltering',
						component: () => import('/@/views/pages/filtering/index.vue'),
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
						/**
						 * 注意此处详情写法：
						 * 1、嵌套进父级里时，面包屑显示为：首页/页面/过滤筛选组件/过滤筛选组件详情
						 * 2、不嵌套进父级时，面包屑显示为：首页/页面/过滤筛选组件/过滤筛选组件详情
						 * 3、想要父级不高亮，面包屑显示为：首页/页面/过滤筛选组件详情，设置路径为：/pages/filteringDetails
						 */
						children: [
							{
								path: '/pages/filtering/details',
								name: 'pagesFilteringDetails',
								component: () => import('/@/views/pages/filtering/details.vue'),
								meta: {
									title: 'message.router.pagesFilteringDetails',
									isLink: '',
									isHide: true,
									isKeepAlive: false,
									isAffix: false,
									isIframe: false,
									roles: ['admin', 'common'],
									icon: 'ele-Sunny',
								},
							},
						],
					},
					{
						path: '/pages/filtering/details1',
						name: 'pagesFilteringDetails1',
						component: () => import('/@/views/pages/filtering/details1.vue'),
						meta: {
							title: 'message.router.pagesFilteringDetails1',
							isLink: '',
							isHide: true,
							isKeepAlive: false,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Sunny',
						},
					},
					{
						path: '/pages/iocnfont',
						name: 'pagesIocnfont',
						component: () => import('/@/views/pages/iocnfont/index.vue'),
						meta: {
							title: 'message.router.pagesIocnfont',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Present',
						},
					},
					{
						path: '/pages/element',
						name: 'pagesElement',
						component: () => import('/@/views/pages/element/index.vue'),
						meta: {
							title: 'message.router.pagesElement',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Eleme',
						},
					},
					{
						path: '/pages/awesome',
						name: 'pagesAwesome',
						component: () => import('/@/views/pages/awesome/index.vue'),
						meta: {
							title: 'message.router.pagesAwesome',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-SetUp',
						},
					},
					{
						path: '/pages/formAdapt',
						name: 'pagesFormAdapt',
						component: () => import('/@/views/pages/formAdapt/index.vue'),
						meta: {
							title: 'message.router.pagesFormAdapt',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-biaodan',
						},
					},
					{
						path: '/pages/tableRules',
						name: 'pagesTableRules',
						component: () => import('/@/views/pages/tableRules/index.vue'),
						meta: {
							title: 'message.router.pagesTableRules',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-jiliandongxuanzeqi',
						},
					},
					{
						path: '/pages/formI18n',
						name: 'pagesFormI18n',
						component: () => import('/@/views/pages/formI18n/index.vue'),
						meta: {
							title: 'message.router.pagesFormI18n',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-diqiu',
						},
					},
					{
						path: '/pages/formRules',
						name: 'pagesFormRules',
						component: () => import('/@/views/pages/formRules/index.vue'),
						meta: {
							title: 'message.router.pagesFormRules',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-shuxing',
						},
					},
					{
						path: '/pages/listAdapt',
						name: 'pagesListAdapt',
						component: () => import('/@/views/pages/listAdapt/index.vue'),
						meta: {
							title: 'message.router.pagesListAdapt',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-chazhaobiaodanliebiao',
						},
					},
					{
						path: '/pages/waterfall',
						name: 'pagesWaterfall',
						component: () => import('/@/views/pages/waterfall/index.vue'),
						meta: {
							title: 'message.router.pagesWaterfall',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-zidingyibuju',
						},
					},
					{
						path: '/pages/steps',
						name: 'pagesSteps',
						component: () => import('/@/views/pages/steps/index.vue'),
						meta: {
							title: 'message.router.pagesSteps',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-step',
						},
					},
					{
						path: '/pages/preview',
						name: 'pagesPreview',
						component: () => import('/@/views/pages/preview/index.vue'),
						meta: {
							title: 'message.router.pagesPreview',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-15tupianyulan',
						},
					},
					{
						path: '/pages/waves',
						name: 'pagesWaves',
						component: () => import('/@/views/pages/waves/index.vue'),
						meta: {
							title: 'message.router.pagesWaves',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-bolangneng',
						},
					},
					{
						path: '/pages/tree',
						name: 'pagesTree',
						component: () => import('/@/views/pages/tree/index.vue'),
						meta: {
							title: 'message.router.pagesTree',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-shuxingtu',
						},
					},
					{
						path: '/pages/drag',
						name: 'pagesDrag',
						component: () => import('/@/views/pages/drag/index.vue'),
						meta: {
							title: 'message.router.pagesDrag',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Pointer',
						},
					},
					{
						path: '/pages/lazyImg',
						name: 'pagesLazyImg',
						component: () => import('/@/views/pages/lazyImg/index.vue'),
						meta: {
							title: 'message.router.pagesLazyImg',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-PictureFilled',
						},
					},
					{
						path: '/pages/dynamicForm',
						name: 'pagesDynamicForm',
						component: () => import('/@/views/pages/dynamicForm/index.vue'),
						meta: {
							title: 'message.router.pagesDynamicForm',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'iconfont icon-wenducanshu-05',
						},
					},
					{
						path: '/pages/workflow',
						name: 'pagesWorkflow',
						component: () => import('/@/views/pages/workflow/index.vue'),
						meta: {
							title: 'message.router.pagesWorkflow',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'ele-Connection',
						},
					},
				],
			},
			{
				path: '/make',
				name: 'makeIndex',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/make/selector',
				meta: {
					title: 'message.router.makeIndex',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin'],
					icon: 'iconfont icon-siweidaotu',
				},
				children: [
					{
						path: '/make/selector',
						name: 'makeSelector',
						component: () => import('/@/views/make/selector/index.vue'),
						meta: {
							title: 'message.router.makeSelector',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-xuanzeqi',
						},
					},
					{
						path: '/make/noticeBar',
						name: 'makeNoticeBar',
						component: () => import('/@/views/make/noticeBar/index.vue'),
						meta: {
							title: 'message.router.makeNoticeBar',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'ele-Bell',
						},
					},
					{
						path: '/make/svgDemo',
						name: 'makeSvgDemo',
						component: () => import('/@/views/make/svgDemo/index.vue'),
						meta: {
							title: 'message.router.makeSvgDemo',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'fa fa-thumbs-o-up',
						},
					},
					{
						path: '/make/tableDemo',
						name: 'makeTableDemo',
						component: () => import('/@/views/make/tableDemo/index.vue'),
						meta: {
							title: 'message.router.makeTableDemo',
							isLink: '',
							isHide: false,
							isKeepAlive: true,
							isAffix: false,
							isIframe: false,
							roles: ['admin', 'common'],
							icon: 'iconfont icon-shuju',
						},
					},
				],
			},
			{
				path: '/visualizing',
				name: 'visualizingIndex',
				component: () => import('/@/layout/routerView/parent.vue'),
				redirect: '/visualizing/visualizingLinkDemo1',
				meta: {
					title: 'message.router.visualizingIndex',
					isLink: '',
					isHide: false,
					isKeepAlive: true,
					isAffix: false,
					isIframe: false,
					roles: ['admin'],
					icon: 'ele-ChatLineRound',
				},
				/**
				 * 打开内置全屏
				 * component 都为 `() => import('/@/layout/routerView/link.vue')`
				 * isLink 链接为内置的路由地址，地址为 staticRoutes 中定义
				 */
				children: [
					{
						path: '/visualizing/visualizingLinkDemo1',
						name: 'visualizingLinkDemo1',
						component: () => import('/@/layout/routerView/link.vue'),
						meta: {
							title: 'message.router.visualizingLinkDemo1',
							isLink: '/visualizingDemo1',
							isHide: false,
							isKeepAlive: false,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'iconfont icon-caozuo-wailian',
						},
					},
					{
						path: '/visualizing/visualizingLinkDemo2',
						name: 'visualizingLinkDemo2',
						component: () => import('/@/layout/routerView/link.vue'),
						meta: {
							title: 'message.router.visualizingLinkDemo2',
							isLink: '/visualizingDemo2',
							isHide: false,
							isKeepAlive: false,
							isAffix: false,
							isIframe: false,
							roles: ['admin'],
							icon: 'iconfont icon-caozuo-wailian',
						},
					},
				],
			},
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
