program NestedDemo;
var
  x: integer;

procedure Outer(p: integer);
var
  y: integer;

  procedure Inner(z: integer);
  begin
    WriteLn('inner=', z);
  end;

begin
  y := p + 1;
  Inner(y);
end;

function Add(a, b: integer): integer;
begin
  Add := a + b;
end;

begin
  x := Add(2, 3);
  Outer(x);
end.
