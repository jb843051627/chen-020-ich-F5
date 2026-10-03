-- ich 非物质文化遗产代表性项目与代表性传承人管理服务 -- schema (chen-020)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_020

-- 2026-10-02 起：t_ich_cycle_task 新增 biz_type/target_code/contact/done_at/fail_reason 五列；
-- 办妥要留两个痕迹（去向翻办妥 + 办妥那一刻），催不动缘由顺着写进同一条；
-- 两路送达与逾期撤回另立 t_ich_cycle_log 一笔一记录，撤回一笔盖不住 due_at 与 done_at 两处旧字。
CREATE TABLE IF NOT EXISTS t_ich_cycle_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '履约考核到期单',
  biz_type int DEFAULT NULL COMMENT '事由 0履约催办 1年度考核',
  target_code varchar(64) DEFAULT NULL COMMENT '所拴名录项目代号或传承人代号',
  contact varchar(64) DEFAULT NULL COMMENT '联系人那一栏（手机）',
  due_at datetime DEFAULT NULL COMMENT '该出手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '可提前几日开口',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对名录项目或传承人记要',
  status int DEFAULT NULL COMMENT '条目情形 0候办 1已办妥 2催不动',
  done_at datetime DEFAULT NULL COMMENT '办妥的那一刻（只随去向翻办妥一同落笔）',
  fail_reason varchar(255) DEFAULT NULL COMMENT '催不动卡在哪一处缘由',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履约考核到期单';

-- 到期单办理留痕：两路送达各记各的（手机短信那一路不算替站内那一路交差），
-- 逾期撤回另记一笔，事后查得到它的存在；旧轮痕迹一笔不抹。
CREATE TABLE IF NOT EXISTS t_ich_cycle_log (
  id bigint NOT NULL COMMENT '主键',
  task_id bigint NOT NULL COMMENT '所属到期单 t_ich_cycle_task.id',
  action varchar(16) DEFAULT NULL COMMENT '动作 SITE_MSG站内送达 SMS手机短信送达 WITHDRAW逾期撤回',
  content varchar(255) DEFAULT NULL COMMENT '这一笔的记要',
  action_time datetime DEFAULT NULL COMMENT '落笔时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_cycle_log_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履约考核到期单办理留痕';

CREATE TABLE IF NOT EXISTS t_ich_grant_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '工坊认定核准单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（县文旅/市文旅/省文旅）',
  round_no int DEFAULT '0' COMMENT '当下签批轮次 0头一回起 每挪回一格加一（新旧两份分开留档）',
  sign_mode int DEFAULT NULL COMMENT '同档并签办法 0一笔即可 1两笔点齐（仅页面参考，凑齐与否以落名记录回算为准）',
  need_count int DEFAULT NULL COMMENT '本档应落名数（仅页面参考，权威数以三档定法回算为准）',
  sign_count int DEFAULT NULL COMMENT '本档已落名数（仅页面参考，权威数顺着落名记录一笔笔点出）',
  status int DEFAULT NULL COMMENT '核准情形 0在核 1已认下 2已挪回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工坊认定核准单';

-- 2026-10-02 起：t_ich_grant_bill 新增 round_no（挪回轮次）；
-- 落名不再只靠格子里的 sign_count，另立 t_ich_grant_sign 一笔一记录，回算以它为准。
-- 落名一笔一记录：凑齐与否、当前档口、复查总笔数一律顺着这张表点出来，
-- 不由谁往 t_ich_grant_bill 的格子里敲数。挪回只退一格，旧轮落名一笔不抹。
CREATE TABLE IF NOT EXISTS t_ich_grant_sign (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属核准单 t_ich_grant_bill.id',
  node_no int NOT NULL COMMENT '落在哪一档 0县文旅 1市文旅 2省文旅',
  round_no int NOT NULL COMMENT '落名时所属轮次 0头一轮起 挪回后续签轮次加一',
  approver varchar(64) NOT NULL COMMENT '落名同志',
  comment varchar(500) DEFAULT NULL COMMENT '签批意见',
  sign_time datetime DEFAULT NULL COMMENT '落名时刻（新旧两份分开留档）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_round_node_approver (bill_id, round_no, node_no, approver)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工坊认定核准单落名记录';

CREATE TABLE IF NOT EXISTS t_ich_image_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '技艺影像卷卷号',
  node_no int DEFAULT NULL COMMENT '本卷版次序',
  site_id int DEFAULT NULL COMMENT '所属名录项目',
  site_no varchar(64) DEFAULT NULL COMMENT '所属项目代号',
  plan_qty decimal(12,2) DEFAULT NULL COMMENT '应归影像件数',
  real_qty decimal(12,2) DEFAULT NULL COMMENT '实归影像件数',
  dev_rate decimal(12,2) DEFAULT NULL COMMENT '两数折出的偏差率',
  content varchar(255) DEFAULT NULL COMMENT '随卷交来的门类要件',
  status int DEFAULT NULL COMMENT '建卷进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技艺影像卷立卷单';

-- 名录项目申报单：四格流转（形式核验/专家评议/社会公示/列入名录）。
-- 2026-10-03 起：格次不由格子里的数说了算，由服务层那一个推进方法顺着已过之格回算
-- （stage 只作页面参考列，权威数以 t_ich_item_flow_record 逐格留痕点出）；
-- 收口有三说：列入/注销/终止，收口即锁档，卷面改不动、整张删不掉。
-- 一份申报（declare_no）底下只容一张在跑的单，头一张没办完也没喊停，后一张立不住。
-- 每格的门槛材料齐不齐、公示日子走没走完，一律钉在留痕表的那一格里。
CREATE TABLE IF NOT EXISTS t_ich_item_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '名录项目申报单号',
  declare_no varchar(64) DEFAULT NULL COMMENT '同一份申报的归口号（一号只容一张在跑的单）',
  stage int DEFAULT NULL COMMENT '当前格次参考列 0..3（核验/评议/公示/列入；权威数以留痕回算为准）',
  status int DEFAULT NULL COMMENT '申领会落 0未起 1在办 2已收口',
  close_outcome int DEFAULT NULL COMMENT '收口说法 1列入 2注销 3终止；未收口空着',
  item_name varchar(255) DEFAULT NULL COMMENT '项目名称（形式核验四样之一）',
  category varchar(32) DEFAULT NULL COMMENT '门类（民间文学/传统技艺/传统医药/传统音乐）',
  apply_area varchar(255) DEFAULT NULL COMMENT '申报地（形式核验四样之一）',
  protect_unit varchar(255) DEFAULT NULL COMMENT '保护单位（形式核验四样之一）',
  public_days int DEFAULT NULL COMMENT '当地定的公示几日（公示格的门槛）',
  public_start datetime DEFAULT NULL COMMENT '公示起笔那一刻（日子没走完材料再齐也不算）',
  expert_ok int DEFAULT NULL COMMENT '专家意见收没收齐 0没收齐 1收齐',
  meeting_ok int DEFAULT NULL COMMENT '名录会议认不认 0不认 1认',
  current_version int DEFAULT '1' COMMENT '当下版次 1头一版起 变更另起一版',
  listed_flag int DEFAULT '0' COMMENT '现行名录上露不露这版 0不露(旧版/未列入) 1露(最新列入版)',
  entry_code varchar(64) DEFAULT NULL COMMENT '列入校验码（一旦列入即钉死，任谁都覆写不了；改版重算）',
  project_id bigint DEFAULT NULL COMMENT '列入后所落名录底册项目 t_ich_project.id',
  cancel_reason varchar(500) DEFAULT NULL COMMENT '列入之后又注销的缘由（卷面留档，年末凭它追为何注销）',
  cancel_time datetime DEFAULT NULL COMMENT '列入之后注销那一刻（列入事实与校验码不抹）',
  content varchar(255) DEFAULT NULL COMMENT '一格一记（同格第二遍不另起一行，留头一遍那句）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除（收口锁档后删不掉）',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_item_flow_declare (declare_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目申报单';

-- 一格一记：每格头一回上报写一笔；同格第二次上报不另起一行，仍留头一遍那句。
-- 后退一格把该格先前所记压到下面（pressed=1），重走从这格再从头攒（另起一笔）。
-- 已过之格全顺着这张表点出：单据停在第几格、后一格收不收，只由服务层推进方法据它回话。
CREATE TABLE IF NOT EXISTS t_ich_item_flow_record (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属申报单 t_ich_item_flow.id',
  stage int NOT NULL COMMENT '落在哪一格 0核验 1评议 2公示 3列入',
  round_no int NOT NULL COMMENT '重走轮次 0头一回起 每退回该格再走加一',
  record_text varchar(255) DEFAULT NULL COMMENT '头一遍上报那句（第二遍不另起一行）',
  pass_flag int DEFAULT '0' COMMENT '0卷面在报 1已过口（点已过之格只点已过口的现行笔）',
  pressed int DEFAULT '0' COMMENT '是否被后退压到下面 0现行 1已压下',
  materials varchar(1000) DEFAULT NULL COMMENT '本格门槛材料齐否的逐条钉档（JSON）',
  enter_time datetime DEFAULT NULL COMMENT '这一笔落格时刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_item_flow_record_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目申报单逐格留痕';

-- 列入之外的变更另起一版：保护单位换了、名称正了字，旧版从现行名录挪开转到往期那一层，
-- 名录上只露最新那一版。每版各算各的校验码，两版显出同一个码便是错的。
CREATE TABLE IF NOT EXISTS t_ich_item_flow_version (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属申报单 t_ich_item_flow.id',
  version_no int NOT NULL COMMENT '版次 1头一版起 每变更一次加一',
  item_name varchar(255) DEFAULT NULL COMMENT '这一版记下的项目名称',
  category varchar(32) DEFAULT NULL COMMENT '这一版记下的门类',
  apply_area varchar(255) DEFAULT NULL COMMENT '这一版记下的申报地',
  protect_unit varchar(255) DEFAULT NULL COMMENT '这一版记下的保护单位',
  entry_code varchar(64) DEFAULT NULL COMMENT '这一版列入那一刻钉死的校验码（新版重算，新旧不重码）',
  current_flag int DEFAULT '0' COMMENT '是否现行名录上露的那一版 0往期 1现行',
  listed_time datetime DEFAULT NULL COMMENT '这一版列入（或变更落版）那一刻',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_item_flow_version_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目申报单变更版次';

CREATE TABLE IF NOT EXISTS t_ich_pre_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '准入门槛线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '起分数值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '过线数值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '优线数值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '让位顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在场 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报准入门槛线';

-- 2026-10-03 起：申报准入两头看（本人从艺年数一条线、带徒人数另一条线管着）。
-- 判定另立回执两张表，每次判定只追加不改写：这一回没过与后来补过是两笔，各留各的；
-- 结论落定时把线代号与启用之日一并钉在明细行里，往后换了线也不回头改这一段。
-- 无可用线（当刻一条也够不着）不给结论：verdict=0，线代号与启用之日两栏空着，
-- 不许抓邻近那条的数凑一个像样的答复。
CREATE TABLE IF NOT EXISTS t_ich_pre_declare (
  id bigint NOT NULL COMMENT '主键',
  declare_no varchar(64) DEFAULT NULL COMMENT '申报批据号（一回判定一笔）',
  applicant_code varchar(64) DEFAULT NULL COMMENT '申报人代号',
  apply_at datetime DEFAULT NULL COMMENT '申请注明那一刻（复算就按这一刻回看线册）',
  verdict int DEFAULT NULL COMMENT '判定情形 0无结论(当刻无可用线) 1准入 2不准入(两头有一头越线)',
  head_total int DEFAULT NULL COMMENT '两头逐条判出的条数（与明细表同回算，恒为2）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_pre_declare_applicant (applicant_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='传承人申报准入判定回执';

-- 一头一行：从艺年数、带徒人数各走各的门槛线，各自落在不在线段内、越了亮该头的数。
-- 线代号与启用之日钉死在这一行：线册后来换了线，这一笔也不回头改。
CREATE TABLE IF NOT EXISTS t_ich_pre_declare_head (
  id bigint NOT NULL COMMENT '主键',
  declare_id bigint NOT NULL COMMENT '所属判定回执 t_ich_pre_declare.id',
  head_no int NOT NULL COMMENT '哪一头 0本人从艺年数 1带徒人数',
  head_name varchar(64) DEFAULT NULL COMMENT '头名（从艺年数/带徒人数）',
  metric decimal(12,2) DEFAULT NULL COMMENT '这一头申报的实际数（年数/人数）',
  rule_code varchar(64) DEFAULT NULL COMMENT '判定那一刻钉下的线代号（无可用线时空着）',
  eff_start datetime DEFAULT NULL COMMENT '判定那一刻钉下的启用之日（无可用线时空着）',
  tier int DEFAULT NULL COMMENT '落到哪一层 1起分层 2过线层 3优线层 4越层；无可用线为空',
  passed int DEFAULT NULL COMMENT '这一头在线段内否 0越线 1过；无可用线为空',
  prompt varchar(500) DEFAULT NULL COMMENT '没过的提示：把越线那一头的分界数与实际数亮出来，不许只回一句没通过',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_pre_declare_head (declare_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报准入两头判定明细';

CREATE TABLE IF NOT EXISTS t_ich_project (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '名录项目代号',
  site_name varchar(128) DEFAULT NULL COMMENT '项目名称全称',
  site_type varchar(32) DEFAULT NULL COMMENT '门类',
  road_name varchar(128) DEFAULT NULL COMMENT '认定到哪一级(县—市—省)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目底册';

CREATE TABLE IF NOT EXISTS t_ich_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '履职情况报送批号',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的传承人代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行进账场数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本行授徒时数',
  status int DEFAULT NULL COMMENT '行落地情形 0未收 1已收下 2已退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履职情况报送核收行';

-- 初始底册数据：两条项目底册，一条在册、一条已注销（注销后不得再挂接新立卷）。
-- t_ich_project 两条种子底册：id=0 在册、id=1 已注销；项目代号与门类按门类口径给值。
INSERT IGNORE INTO t_ich_project (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'XM00', '澜川县·苗绣（银苗绣片技艺，在册）', '传统技艺', '澜川县—河湾区—长街上', 0, 0, 'seed', NOW()),
       (1, 'XM01', '澜川县·侗族大歌（歌师已故项目，已注销）', '传统音乐', '澜川县—河湾区—滨河北', 1, 0, 'seed', NOW());

