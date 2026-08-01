package models

// ---- Day Template ----

type DayTemplate struct {
	ID         string `json:"id"`
	Name       string `json:"name"`
	Icon       string `json:"icon"`
	RepeatDays []int  `json:"repeatDays"`
	CreatedAt  string `json:"createdAt"`
}

type CreateTemplateReq struct {
	Name       string `json:"name"`
	Icon       string `json:"icon"`
	RepeatDays []int  `json:"repeatDays"`
}

type UpdateTemplateReq struct {
	Name       *string `json:"name,omitempty"`
	Icon       *string `json:"icon,omitempty"`
	RepeatDays *[]int  `json:"repeatDays,omitempty"`
}

// ---- Time Block ----

type TimeBlock struct {
	ID          string    `json:"id"`
	TemplateID  string    `json:"templateId"`
	Name        string    `json:"name"`
	Icon        string    `json:"icon"`
	Mode        string    `json:"mode"` // "start_end" | "start_duration"
	StartTime   string    `json:"startTime"`
	EndTime     *string   `json:"endTime,omitempty"`
	DurationMin *int      `json:"durationMin,omitempty"`
	BlockOrder  int       `json:"blockOrder"`
	Subtasks    []Subtask `json:"subtasks,omitempty"`
	CreatedAt   string    `json:"createdAt"`
}

type CreateBlockReq struct {
	Name        string       `json:"name"`
	Icon        string       `json:"icon"`
	Mode        string       `json:"mode"`
	StartTime   string       `json:"startTime"`
	EndTime     *string      `json:"endTime,omitempty"`
	DurationMin *int         `json:"durationMin,omitempty"`
	Subtasks    []SubtaskReq `json:"subtasks,omitempty"`
}

// SubtaskSync is one entry of a full subtask save: an existing subtask id
// (nil = create a new subtask) plus the name. Order in the slice is the new
// subtask order; subtasks not listed are deleted.
type SubtaskSync struct {
	ID   *string `json:"id,omitempty"`
	Name string  `json:"name"`
}

type UpdateBlockReq struct {
	Name        *string       `json:"name,omitempty"`
	Icon        *string       `json:"icon,omitempty"`
	Mode        *string       `json:"mode,omitempty"`
	StartTime   *string       `json:"startTime,omitempty"`
	EndTime     *string       `json:"endTime,omitempty"`
	DurationMin *int          `json:"durationMin,omitempty"`
	Subtasks    []SubtaskSync `json:"subtasks,omitempty"`
}

type ReorderReq struct {
	IDs []string `json:"ids"`
}

// ---- Subtask ----

type Subtask struct {
	ID           string `json:"id"`
	TimeBlockID  string `json:"timeBlockId"`
	Name         string `json:"name"`
	SubtaskOrder int    `json:"subtaskOrder"`
}

type SubtaskReq struct {
	Name string `json:"name"`
}

type UpdateSubtaskReq struct {
	Name *string `json:"name,omitempty"`
}

// ---- Schedule Entry ----

type ScheduleEntry struct {
	ID           string       `json:"id"`
	Date         string       `json:"date"`
	TemplateID   *string      `json:"templateId,omitempty"`
	TemplateName *string      `json:"templateName,omitempty"`
	IsSpecial    bool         `json:"isSpecial"`
	Blocks       []BlockState `json:"blocks,omitempty"`
	CreatedAt    string       `json:"createdAt"`
}

type BlockState struct {
	ID            string         `json:"id"`
	TimeBlockID   string         `json:"timeBlockId"`
	Name          string         `json:"name"`
	Icon          string         `json:"icon"`
	StartTime     string         `json:"startTime"`
	EndTime       *string        `json:"endTime,omitempty"`
	DurationMin   *int           `json:"durationMin,omitempty"`
	Mode          string         `json:"mode"`
	BlockOrder    int            `json:"blockOrder"`
	AutoStatus    string         `json:"autoStatus"`
	ManualStatus  string         `json:"manualStatus"`
	SubtaskStates []SubtaskState `json:"subtaskStates,omitempty"`
}

type SubtaskState struct {
	ID        string `json:"id"`
	SubtaskID string `json:"subtaskId"`
	Name      string `json:"name"`
	Done      bool   `json:"done"`
	Order     int    `json:"order"`
}

type UpdateManualStatusReq struct {
	Status string `json:"status"` // "completed" | "not_completed"
}

type SetTemplateReq struct {
	TemplateID *string `json:"templateId"` // null = özel gün
}
