import { FormRules } from "element-plus";

/**
 * views personal
 */
type NewInfo = {
	title: string;
	date: string;
	link: string;
};
type Recommend = {
	title: string;
	msg: string;
	icon: string;
	bg: string;
	iconColor: string;
};
declare type PersonalState = {
	newsInfoList: NewInfo[];
	recommendList: Recommend[];
	personalForm: {
		name: string;
		email: string;
		autograph: string;
		occupation: string;
		phone: string;
		sex: string;
	};
};

/**
 * views visualizing
 */
declare type Demo2State<T = any> = {
	time: {
		txt: string;
		fun: number;
	};
	dropdownList: T[];
	dropdownActive: string;
	skyList: T[];
	dBtnList: T[];
	chartData4Index: number;
	dBtnActive: number;
	earth3DBtnList: T[];
	chartData4List: T[];
	myCharts: T[];
};

/**
 * views params
 */
declare type ParamsState = {
	value: string;
	tagsViewName: string;
	tagsViewNameIsI18n: boolean;
};

/**
 * views system
 */
// role
declare interface RowRoleType {
	roleName: string;
	roleSign: string;
	describe: string;
	sort: number;
	status: boolean;
	createTime: string;
}

interface SysRoleTableType extends TableType {
	data: RowRoleType[];
}

declare interface SysRoleState {
	tableData: SysRoleTableType;
}

declare type TreeType = {
	id: number;
	label: string;
	children?: TreeType[];
};

// user
declare type RowUserType<T = any> = {
	userName: string;
	userNickname: string;
	roleSign: string;
	department: string[];
	phone: string;
	email: string;
	sex: string;
	password: string;
	overdueTime: T;
	status: boolean;
	describe: string;
	createTime: T;
};

interface SysUserTableType extends TableType {
	data: RowUserType[];
}

declare interface SysUserState {
	tableData: SysUserTableType;
}

declare type DeptTreeType = {
	deptName: string;
	createTime: string;
	status: boolean;
	sort: number;
	describe: string;
	id: number | string;
	children?: DeptTreeType[];
};

// dept
declare interface RowDeptType extends DeptTreeType {
	deptLevel: string[];
	person: string;
	phone: string;
	email: string;
}

interface SysDeptTableType extends TableType {
	data: DeptTreeType[];
}

declare interface SysDeptState {
	tableData: SysDeptTableType;
}

// dic
type ListType = {
	id: number;
	label: string;
	value: string;
};

declare interface RowDicType {
	dicName: string;
	fieldName: string;
	describe: string;
	status: boolean;
	createTime: string;
	list: ListType[];
}

interface SysDicTableType extends TableType {
	data: RowDicType[];
}

declare interface SysDicState {
	tableData: SysDicTableType;
}

/**
 * views pages
 */
//  filtering
declare type FilteringChilType = {
	id: number | string;
	label: string;
	active: boolean;
};

declare type FilterListType = {
	img: string;
	title: string;
	evaluate: string;
	collection: string;
	price: string;
	monSales: string;
	id: number | string;
	loading?: boolean;
};

declare type FilteringRowType = {
	title: string;
	isMore: boolean;
	isShowMore: boolean;
	id: number | string;
	children: FilteringChilType[];
};

// tableRules
declare type TableRulesHeaderType = {
	prop: string;
	width: string | number;
	label: string;
	isRequired?: boolean;
	isTooltip?: boolean;
	type: string;
};

declare type TableRulesState = {
	tableData: {
		data: EmptyObjectType[];
		header: TableRulesHeaderType[];
		option: SelectOptionType[];
	};
};

declare type TableRulesOneProps = {
	name: string;
	email: string;
	autograph: string;
	occupation: string;
};

// tree
declare type RowTreeType = {
	id: number;
	label: string;
	label1: string;
	label2: string;
	isShow: boolean;
	children?: RowTreeType[];
};

// workflow index
declare type NodeListState = {
	id: string | number;
	nodeId: string | undefined;
	class: HTMLElement | string;
	left: number | string;
	top: number | string;
	icon: string;
	name: string;
};

declare type LineListState = {
	sourceId: string;
	targetId: string;
	label: string;
};

declare type XyState = {
	x: string | number;
	y: string | number;
};

declare type WorkflowState<T = any> = {
	leftNavList: T[];
	dropdownNode: XyState;
	dropdownLine: XyState;
	isShow: boolean;
	jsPlumb: T;
	jsPlumbNodeIndex: null | number;
	jsplumbDefaults: T;
	jsplumbMakeSource: T;
	jsplumbMakeTarget: T;
	jsplumbConnect: T;
	jsplumbData: {
		nodeList: NodeListState[];
		lineList: LineListState[];
	};
};

// workflow drawer
declare type WorkflowDrawerNodeState<T = any> = {
	node: { [key: string]: T };
	nodeRules: T;
	form: T;
	tabsActive: string;
	loading: {
		extend: boolean;
	};
};

declare type WorkflowDrawerLabelType = {
	type: string;
	label: string;
};

declare type WorkflowDrawerState<T = any> = {
	isOpen: boolean;
	nodeData: {
		type: string;
	};
	jsplumbConn: T;
};

/**
 * views make
 */
// tableDemo
declare type TableDemoPageType = {
	current: number;
	size: number;
};

declare type TableHeaderType = {
	key: string;
	width: string;
	title: string;
	type: string | number;
	colWidth: string;
	width?: string | number;
	height?: string | number;
	isCheck: boolean;
};

declare type TableSearchType = {
	label: string;
	prop: string;
	placeholder: string;
	required: boolean;
	type: string;
	options?: SelectOptionType[];
};

declare type TableDemoState = {
	tableData: {
		data: EmptyObjectType[];
		header: TableHeaderType[];
		config: {
			total: number;
			loading: boolean;
			isBorder: boolean;
			isSelection: boolean;
			isSerialNo: boolean;
			isOperate: boolean;
		};
		search: TableSearchType[];
		param: EmptyObjectType;
		printName: string;
	};
};

// data category
declare type DataCategory = {
	[key: string]: any;
	id: string;
	dataName: string;
	dataLabel: string;
	description: string;
	typeCode: string;
	fixCode: string;
	floatCode: string;
	dataCode: string;
	nodeLevel: number;
	parentId: string;
	children: DataCategory[];
	createTime: string;
	updateTime: string;
}


declare interface CategoryTreeTable<T = DataCategory> extends TableType{
	categoryTree: T[];
	dataList: T[];
	expandedKeys: string[];
	currentNodeKey: string;
	treeNodeSearch: string;
	currentNode: T;
	defaultProps: any;
}


// 采集Api
declare type CollectionApi = {
	id: string;
	name: string;
	title: string;
	description: string;
	status: string;
	type: string;
	typeMethod: string;
	source: string;
	sourcePage: string;
	dataCategoryId: string;
	dataCategoryName: string;
	requestParams: ApiParam[];
	deletedParams: ApiParam[];
	responseParams: ApiParam[];
	createTime: string;
	updateTime: string;
};

// api详情页面
declare type ApiInfo<T> = {
	api: T;
	rules: FormRules;
	param: any;
}

// Api参数，position：0 请求参数， 1 返回参数
declare type ApiParam = {
	id: string;
	apiId: string;
	paramName: string;
	paramType: string;
	description: string;
	notNull: number;
	example: string;
	position: number;
	sortNum: number;
	editFlag: number;
}

// 采集Api 列表
interface EntityTableType<T> extends TableType {
	data: T[];
}

declare interface EntityDialog<T> {
	entityForm: T;
	dialog: {
		isShowDialog: boolean,
		type: string,
		title: string,
		submitTxt: string,
		editFlag: number,
	};
	rules: FormRules;
}

declare interface EntityState<T> {
	tableData: EntityTableType<T>;
}

declare type CollectionTask = {
	id: string;
	apiId: string;
	taskName: string;
	apiName: string;
	taskDescription: string;
	dataCategory: string;
	scheduleCycle: string;
	status: number;
	params: any;
	createTime: string;
	updateTime: string;
}

declare type DataType = {
	[key: string]: any;
	id: string;
	typeName: string;
	typeLabel: string;
	description: string;
	parentId: string;
	nodeType: string;
	children: DataType[];
	createTime: string;
	updateTime: string;
}

declare type DataTypeAttr = {
	id: string;
	dataTypeId: string;
	name: string;
	type: string;
	length: number;
	description: string;
	required: number;
	createTime: string;
	updateTime: string;
}
